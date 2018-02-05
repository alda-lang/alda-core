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

(defn take-chord
  "Given a sequence of events AFTER an initial note, takes the first N events that can form a chord with the first note."
  ([input-events]
   (take-chord input-events []))
  ([[x & more] chord-events]
   (if (or (not x)
           (and (#{:note :rest} (:event-type x)) (not (:chord? x)))
           (not (#{:note :rest :attribute-change} (:event-type x))))
     (if (some #(#{:note :rest} (:event-type %)) chord-events)
       chord-events
       ())
     (recur more (conj chord-events x)))))

(defn aggregate-inner-events
  ([input-events]
   (aggregate-inner-events input-events []))
  ([[x & more] events]
   (cond
     (not x)
     events

     (sequential? x)
     (recur more (conj events (aggregate-inner-events x)))

     (:chord? x)
     (throw (Exception. "No previous note with which to create a chord."))

     (:events x)
     (let [aggregated-x (->> (aggregate-inner-events (:events x))
                             (assoc x :events))]
       (recur more (conj events aggregated-x)))

     (not (#{:note :rest} (:event-type x)))
     (recur more (conj events x))

     :else
     (let [maybe-chord (take-chord more)]
       (if (empty? maybe-chord)
         (recur more (conj events x))
         (let [chord (->> (cons x maybe-chord)
                          (map #(if (map? %) (dissoc % :chord?) %))
                          (apply event/chord))]
           (recur (drop (count maybe-chord) more)
                  (conj events chord))))))))

(defn flush-buffer!
  [{:keys [buffer] :as parser}]
  (doseq [event (aggregate-inner-events buffer)]
    (emit-event! parser event))
  (-> parser (update :buffer empty)))

(defn push-event
  [{:keys [buffer] :as parser} event]
  (cond
    (instance? Throwable event)
    (-> parser (emit-event! event))

    (= :EOF event)
    (-> parser flush-buffer!)

    (#{:note :rest} (:event-type event))
    (if (or (empty? buffer) (:chord? event))
      (-> parser (add-to-buffer event))
      (-> parser flush-buffer! (add-to-buffer event)))

    (= :attribute-change (:event-type event))
    (if (empty? buffer)
      (-> parser (emit-event! event))
      (-> parser (add-to-buffer event)))

    (#{:cram :set-variable} (:event-type event))
    (let [events (aggregate-inner-events (:events event))
          event  (assoc event :events events)]
      (-> parser flush-buffer! (emit-event! event)))

    (sequential? event)
    (let [events (aggregate-inner-events event)]
      (-> parser flush-buffer! (emit-event! events)))

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
