(ns alda.parser.aggregate-events
  (:require [clojure.core.async :refer (>!!)]
            [alda.lisp.events   :as    event]))

(defn initial-parser-state
  []
  {:state :parsing
   :buffer []})

(defn parser
  [events-ch]
  (-> (initial-parser-state)
      (assoc :events-ch events-ch)))

(defn emit-event!
  [{:keys [events-ch] :as parser} event]
  (when-not (= :EOF event)
    (>!! events-ch event))
  parser)

(defn add-to-buffer
  [parser event]
  (-> parser (update :buffer conj event)))

(defn flush-buffer!
  [{:keys [buffer] :as parser}]
  (if (some :chord? buffer)
    (let [chord (apply event/chord buffer)]
      (emit-event! parser chord))
    (doseq [event buffer]
      (emit-event! parser event)))
  (-> parser (update :buffer empty)))

(defn push-event
  [{:keys [buffer] :as parser} event]
  (cond
    (instance? Throwable event)
    (-> parser (emit-event! event))

    (empty? buffer)
    (if (#{:note :rest} (:event-type event))
      (-> parser (add-to-buffer event))
      (-> parser (emit-event! event)))

    (#{:note :rest} (:event-type event))
    (if (:chord? event)
      (-> parser (add-to-buffer event))
      (-> parser flush-buffer! (add-to-buffer event)))

    :else
    (-> parser flush-buffer! (emit-event! event))))

(defn ensure-parsing
  "If the parser's state is not :parsing, short-circuits the parser so that the
   current state is passed through until the end.

   Otherwise returns nil so that parsing will continue."
  [{:keys [state] :as parser}]
  (when (not= :parsing state)
    parser))

(defn read-event!
  "Reads one event `t` and updates parser `p`.

   Puts events on (:events-ch p) as they are read and (possibly) aggregated."
  [p e]
  (try
    (or (ensure-parsing p)
        (push-event p e))
    (catch Throwable e
      (push-event p e))))
