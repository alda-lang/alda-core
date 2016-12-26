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
            [alda.parser.tokenize     :as    token]))

(defn parse-input
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
            (>!! tokens-ch (dissoc parser :tokens-ch))
            (close! tokens-ch)))))

    ; temp: print out tokens as they are parsed
    (thread
      (loop []
        (when-let [token (<!! tokens-ch)]
          (prn token)
          (recur))))))

