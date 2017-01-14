
(ns alda.parser.parse-events
  (:require [clojure.core.async :refer (>!!)]))

(def initial-parser-state
  {:state          :parsing ; parsing, done, or error
   ; TODO
   })

(defn emit-event!
  [{:keys [events-ch] :as parser} event]
  (>!! events-ch event)
  parser)

(defn parser
  [events-ch]
  (assoc initial-parser-state :events-ch events-ch))

(defn ensure-parsing
  "If the parser's state is not :parsing, short-circuits the parser so that the
   current state is passed through until the end.

   Otherwise returns nil so that parsing will continue."
  [{:keys [state] :as parser}]
  (when (not= :parsing state)
    parser))

(defn finish-parsing
  [parser character]
  (when (= :EOF character)
    (-> parser (assoc :state :done))))

(defn caught-error
  [parser e]
  (-> parser (emit-event! e) (assoc :state :error)))

(defn unexpected-token-error
  [parser token]
  (let [error-msg (format "Unexpected token: %s." token)]
    (-> parser (emit-event! (Exception. error-msg)) (assoc :state :error))))

(defn read-token!
  "Reads one token `t` and updates parser `p`.

   Puts events on (:events-ch p) as they are parsed."
  [p t]
  (try
    (or (ensure-parsing p)
        (finish-parsing p t)
        (unexpected-token-error p t))
    (catch Throwable e
      (caught-error p t))))
