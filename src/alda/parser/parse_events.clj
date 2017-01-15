(ns alda.parser.parse-events
  (:require [clojure.core.async   :refer (>!!)]
            [alda.parser.tokenize :refer (token-names)]))

(defn initial-parser-state
  [& [initial-context]]
  {:state :parsing
   :stack [[(or initial-context :header)]] ; context for nesting events
   })

(defn emit-event!
  [{:keys [events-ch] :as parser} event]
  (>!! events-ch event)
  parser)

(defn parser
  [events-ch & [initial-context]]
  (-> (initial-parser-state initial-context)
      (assoc :events-ch events-ch)))

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
  (let [error-msg (if (sequential? token)
                    (let [[token [line column] content] token]
                      (format "Unexpected %s at line %s, column %s."
                              (if (= :EOF token)
                                "EOF"
                                (get token-names token token))
                              line
                              column))
                    (format "Unexpected token: %s." token))]
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
