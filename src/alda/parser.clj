(ns alda.parser
  (:require [alda.lisp.score :as score]
            [alda.parser
             [aggregate-events :as agg]
             [parse-events     :as event]
             [tokenize         :as token]]
            [clojure.core.async :refer [<!! >!! chan close! go-loop thread]]))

(defn print-stream
  "Continuously reads from a channel and prints what is received, stopping once
   the channel is closed."
  [channel]
  (thread
    (loop []
      (when-let [x (<!! channel)]
        (prn x)
        (recur)))))

(defn stream-seq
  "Coerces a channel into a lazy sequence that can be lazily consumed or
   realized at will."
  [ch]
  (lazy-seq (when-let [v (<!! ch)] (cons v (stream-seq ch)))))

(defn tokenize
  "Asynchronously reads and tokenizes input, streaming the result into a
   channel.

   Returns a channel from which tokens can be read as they are parsed."
  [input]
  (let [chars-ch  (chan)
        tokens-ch (chan)]
    ; feed each character of input to chars-ch
    (thread
      (doseq [character input] (>!! chars-ch character))
      (>!! chars-ch :EOF)
      (close! chars-ch))

    ; parse tokens from chars-ch and feed them to tokens-ch
    (thread
      (loop [{:keys [line column] :as parser} (token/parser tokens-ch)]
        (if-let [character (<!! chars-ch)]
          (recur (token/read-character! parser character))
          (do
            (>!! tokens-ch [:EOF [line column]])
            (close! tokens-ch)))))

    tokens-ch))

(defn parse-events
  "Asynchronously reads tokens from a channel, parsing events and streaming
   them into a new channel.

   Returns a channel from which events can be read as they are parsed.

   If there is an error, the error is included in the stream."
  [tokens-ch]
  ;; alda.lisp must be required and referred in order to use inline Clojure
  ;; expressions.
  (when-not (resolve 'ALDA-LISP-LOADED)
    (require '[alda.lisp :refer :all]))
  (let [events-ch (chan)]
    (thread
      (loop [parser (event/parser events-ch)]
        (if-let [token (<!! tokens-ch)]
          (recur (event/read-token! parser token))
          (do
            (>!! events-ch :EOF)
            (close! events-ch)))))
    events-ch))

(defn aggregate-events
  "Asynchronously reads events from a channel and aggregates certain types of events that need to be aggregated, e.g. notes in a chord.

   Returns a channel on which the final events can be read.

   If there is an error, the error is included in the stream."
  [events-ch]
  ;; alda.lisp must be required and referred in order to use inline Clojure
  ;; expressions.
  (when-not (resolve 'ALDA-LISP-LOADED)
    (require '[alda.lisp :refer :all]))
  (let [events-ch2 (chan)]
    (thread
      (loop [parser (agg/parser events-ch2)]
        (if-let [event (<!! events-ch)]
          (recur (agg/read-event! parser event))
          (close! events-ch2))))
    events-ch2))

(defn build-score
  "Asynchronously reads events from a channel and applies them sequentially to a
   new score.

   Returns a channel from which the complete score can be taken.

   If there was an error in the a previous part of the pipeline, it is thrown
   here."
  [events-ch2]
  ;; alda.lisp must be required and referred in order to use inline Clojure
  ;; expressions.
  (when-not (resolve 'ALDA-LISP-LOADED)
    (require '[alda.lisp :refer :all]))
  (go-loop [score (score/score), error nil]
    (let [event (<!! events-ch2)]
      (cond
        (nil? event)
        (or error score)

        (instance? Throwable event)
        (recur score event)

        error
        (recur score error)

        :else
        (let [[score error] (try
                              [(score/continue score event) nil]
                              (catch Throwable e
                                [score e]))]
          (recur score error))))))

(defn parse-input
  "Given a string of Alda code, process it via the following asynchronous
   pipeline:

   - Tokenize it into a stream of recognized tokens.
   - From the token stream, parse out a stream of events.
   - Process the events sequentially to build a score.

   If an :output key is supplied, the result will depend on the value of that
   key:

   :score => an Alda score map, ready to be performed by the sound engine. If
   there is an error, the error is thrown.

   :events => a lazy sequence of Alda events, which will produce a complete
   score when applied sequentially to a new score. Note that the sequence may
   contain an error object if there is any error parsing, and the error is not
   thrown. If it is desirable to throw an error, use :events-or-error.

   :events-or-error => equivalent to :events, but the sequence is fully realized
   and an error is thrown in the event of a parse error.

   The default :output is :score."
  [input & {:keys [output] :or {output :score}}]
  (case output
    :score
    (let [score (-> input
                    tokenize
                    parse-events
                    aggregate-events
                    build-score
                    <!!)]
      (if (instance? Throwable score)
        (throw score)
        score))

    :events
    (-> input tokenize parse-events aggregate-events stream-seq)

    :events-or-error
    (let [events (parse-input input :output :events)]
      (doseq [event events]
        (if (instance? Throwable event)
          (throw event)))
      events)))
