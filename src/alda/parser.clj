(ns alda.parser
  (:require [clojure.core.async       :refer (chan thread <!! >!! close!)]
            [clojure.string           :as    str]
            [clojure.java.io          :as    io]
            [taoensso.timbre          :as    log]
            [alda.lisp.attributes     :as    attrs]
            [alda.lisp.events         :as    evts]
            [alda.lisp.model.duration :as    dur]
            [alda.lisp.model.pitch    :as    pitch]
            [alda.lisp.score          :as    score]
            [alda.parser.tokenize     :as    token]
            [alda.parser.parse-events :as    event]))

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
      (loop [parser (token/parser tokens-ch)]
        (if-let [character (<!! chars-ch)]
          (recur (token/read-character! parser character))
          (do
            (>!! tokens-ch :EOF)
            (close! tokens-ch)))))

    tokens-ch))

(defn parse-events
  "Asynchronously reads tokens from a channel, parsing events and streaming
   them into a new channel.

   Returns a channel from which events can be read as they are parsed."
  [tokens-ch]
  (let [events-ch (chan)]
    (thread
      (loop [parser (event/parser events-ch)]
        (if-let [token (<!! tokens-ch)]
          (recur (event/read-token! parser token))
          (do
            (>!! events-ch :EOF)
            (close! events-ch)))))
    events-ch))

(defn parse-input
  [input]
  ; temp: print out tokens as they are parsed
  (-> input tokenize parse-events print-stream))

