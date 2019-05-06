(ns alda.parser.tokenize
  (:require [clojure.core.async :refer (>!!)]))

(def initial-parser-state
  {:state          :parsing ; parsing, done, or error
   :line           1
   :column         1
   :stack          []})       ; context for nesting tokens

(defn parser
  [tokens-ch]
  (assoc initial-parser-state :tokens-ch tokens-ch))

(def token-names
  {:accidentals       "accidentals"
   :clj-char          "Clojure character"
   :clj-sexp          "Clojure S-expression"
   :clj-expr          "Clojure expression"
   :clj-string        "Clojure string"
   :colon             "':'"
   :comment           "comment"
   :duration          "duration"
   :equals            "'='"
   :event-seq         "event sequence"
   :name              "name"
   :nickname          "nickname"
   :note              "note"
   :note-length       "note length"
   :note-rest-or-name "note, rest, or name"
   :octave-change     "octave change"
   :repeat            "repeat"
   :repeat-num        "repeat number"
   :rest              "rest"
   :slash             "'/'"})

(defn current-token-type
  [{:keys [stack] :as parser}]
  (-> stack peek first))

(defn starting-line-and-column
  [{:keys [stack] :as parser}]
  (-> stack peek second))

(defn current-token-content
  [{:keys [stack] :as parser}]
  (-> stack peek (nth 2)))

(defn last-token-type
  [{:keys [stack] :as parser}]
  (-> stack pop peek first))

(defn last-token-content
  [{:keys [stack] :as parser}]
  (-> stack pop peek (nth 2)))

(defn currently-parsing?
  [parser token]
  (= (current-token-type parser) token))

(defn pop-stack
  [parser]
  (-> parser (update :stack #(if (empty? %) % (pop %)))))

(defn emit!
  [{:keys [tokens-ch] :as parser} x]
  (>!! tokens-ch x)
  parser)

(defn emit-token!
  [parser & {:keys [token content pop-stack?]}]
  (let [maybe-pop-stack #(if pop-stack? (pop-stack %) %)]
    (-> parser
        (emit! [(or token (current-token-type parser))
                (starting-line-and-column parser)
                (or content (current-token-content parser))])
        maybe-pop-stack)))

(defn emit-error!
  [parser e-or-msg]
  (let [error (if (instance? Throwable e-or-msg)
                e-or-msg
                (Exception. e-or-msg))]
    (-> parser (emit! error) (assoc :state :error))))

(defn unexpected-char-error
  [{:keys [line column] :as parser} character]
  (let [parsing   (current-token-type parser)
        error-msg (format "Unexpected %s%s at line %s, column %s."
                          (if (= :EOF character)
                            "EOF"
                            (format "'%s'" character))
                          (if parsing
                            (str " in " (get token-names parsing parsing))
                            "")
                          line
                          column)]
    (-> parser (emit-error! error-msg))))

(defn reject-chars
  [parser character blacklist]
  (when (contains? blacklist character)
    (unexpected-char-error parser character)))

(defn next-line
  [parser]
  (-> parser (update :line inc) (assoc :column 1)))

(defn next-column
  [parser]
  (-> parser (update :column inc)))

(defn advance
  [parser x & [size]]
  (cond
    (= :EOF x)
    parser

    (#{\newline "\n"} x)
    (-> parser next-line)

    :else
    (-> parser (update :column + (or size 1)))))

(defn append-to-current-buffer
  [{:keys [stack] :as parser} x]
  (if-let [[token [line col] buffer] (peek stack)]
    (update parser :stack #(-> % pop (conj [token [line col] (str buffer x)])))
    parser))

(defn add-current-buffer-to-last
  [{:keys [stack] :as parser}]
  (let [[current-token current-line-col current-buffer] (peek stack)
        popped-stack                                    (pop stack)
        [last-token last-line-col last-buffer]          (peek popped-stack)
        last-buffer+                                    (str last-buffer
                                                             current-buffer)]
    (-> parser
        (assoc :stack (-> popped-stack pop (conj [last-token
                                                  last-line-col
                                                  last-buffer+]))))))

(defn new-buffer
  [{:keys [line column] :as parser} token]
  (-> parser (update :stack conj [token [line column] ""])))

(defn read-to-buffer
  [parser x & [size]]
  (-> parser (append-to-current-buffer x) (advance x size)))

(defn read-to-new-buffer
  [parser token x & [size]]
  (-> parser (new-buffer token) (read-to-buffer x size)))

(defn rename-current-token
  [{:keys [stack] :as parser} token]
  (let [renamed-token (-> stack peek (assoc 0 token))]
    (-> parser (update :stack #(-> % pop (conj renamed-token))))))

(defn read-chars
  [parser character whitelist]
  (when (contains? whitelist character)
    (-> parser (read-to-buffer character))))

(defn discard-buffer
  [parser]
  (-> parser (update :stack pop)))

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

(defn ignore-carriage-return
  [parser character]
  (when (= \return character)
    parser))

(defn skip-whitespace
  [parser character]
  (when (#{\newline \space \tab} character)
    (advance parser character)))

(declare read-character!)

(defn start-parsing
  [parser character token
   & [{:keys [start-char ignore-first-char buffer-first-char]}]]
  (when (or (nil? start-char)
            (= start-char character)
            (and (set? start-char) (contains? start-char character)))
    (let [maybe-advance #(if ignore-first-char
                           (-> % (advance character))
                           %)
          maybe-buffer  #(if buffer-first-char
                           (-> % (read-to-buffer character))
                           %)
          maybe-read    #(if (or ignore-first-char buffer-first-char)
                           %
                           (-> % (read-character! character)))]
      (-> parser (new-buffer token) maybe-advance maybe-buffer maybe-read))))

(defn start-parsing-accidentals
  [p c]
  (start-parsing p c :accidentals))

(defn start-parsing-at-marker
  [p c]
  (start-parsing p c :at-marker {:start-char \@ :ignore-first-char true}))

(defn start-parsing-clj-char
  [p c]
  (start-parsing p c :clj-char {:start-char \\}))

(defn start-parsing-clj-sexp
  [p c]
  (start-parsing p c :clj-sexp {:start-char \( :buffer-first-char true}))

(defn start-parsing-clj-string
  [p c]
  (start-parsing p c :clj-string {:start-char \" :buffer-first-char true}))

(defn start-parsing-comment
  [p c]
  (start-parsing p c :comment {:start-char \# :ignore-first-char true}))

(defn start-parsing-duration
  [p c]
  (start-parsing p c :duration))

(defn start-parsing-marker
  [p c]
  (start-parsing p c :marker {:start-char \% :ignore-first-char true}))

(defn start-parsing-nickname
  [p c]
  (start-parsing p c :nickname {:start-char \" :ignore-first-char true}))

(defn start-parsing-note-length
  [p c]
  (start-parsing p c :note-length))

(defn start-parsing-note-rest-or-name
  [p c]
  (start-parsing p c :note-rest-or-name
                 {:start-char (set (str "abcdefghijklmnopqrstuvwxyz"
                                        "ABCDEFGHIJKLMNOPQRSTUVWXYZ"))
                  :buffer-first-char true}))

(defn start-parsing-octave-change
  [p c]
  (start-parsing p c :octave-change {:start-char #{\o \< \>}}))

(defn start-parsing-repeat
  [p c]
  (start-parsing p c :repeat {:start-char \* :ignore-first-char true}))

(defn start-parsing-repeat-num
  [p c]
  (start-parsing p c :repeat-num {:start-char \' :ignore-first-char true}))

(defn start-parsing-voice
  [p c]
  (start-parsing p c :voice {:start-char \V}))

(declare parse-tie)
(defn parse-accidentals
  [parser character]
  (when (currently-parsing? parser :accidentals)
    (condp contains? character
      #{\+ \- \_}
      (-> parser (read-to-buffer character))

      #{\0 \1 \2 \3 \4 \5 \6 \7 \8 \9}
      (-> parser (emit-token! :pop-stack? true)
                 (start-parsing-duration character))

      #{\~}
      (-> parser (emit-token! :pop-stack? true)
                 (parse-tie character))

      ; else
      (-> parser (emit-token! :pop-stack? true)
                 (read-character! character)))))

(defn parse-at-marker
  [parser character]
  (when (currently-parsing? parser :at-marker)
    (if ((set (str "abcdefghijklmnopqrstuvwxyz"
                   "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                   "0123456789_-"))
         character)
      (-> parser (read-to-buffer character))
      (-> parser (emit-token! :pop-stack? true)
                 (read-character! character)))))

(defn parse-barline
  [parser character]
  (when (= \| character)
    (-> parser (read-to-new-buffer :barline character)
               (emit-token! :pop-stack? true))))

(defn parse-clj-char
  [parser character]
  (when (currently-parsing? parser :clj-char)
    (cond
      (empty? (current-token-content parser))
      (-> parser (read-to-buffer character))

      ((set "0123456789abcdefghijklmnopqrstuvwxyz") character)
      (-> parser (read-to-buffer character))

      :else
      (-> parser (read-to-buffer character) add-current-buffer-to-last))))

(declare parse-clj-string finish-parsing-clj-sexp)
(defn parse-clj-sexp
  [p c]
  (when (currently-parsing? p :clj-sexp)
    (or (reject-chars p c #{:EOF})
        (read-chars p c #{\newline \space \,})
        (parse-clj-string p c)
        (parse-clj-char p c)
        (start-parsing-clj-sexp p c)
        (start-parsing-clj-string p c)
        (start-parsing-clj-char p c)
        (finish-parsing-clj-sexp p c)
        (read-to-buffer p c))))

(defn finish-parsing-clj-sexp
  [parser character]
  (when (= \) character)
    (let [emit-or-continue-parsing-parent
          (if (= :clj-sexp (last-token-type parser))
            add-current-buffer-to-last
            #(-> % (emit-token! :token :clj-expr :pop-stack? true)))]
      (-> parser (read-to-buffer \)) emit-or-continue-parsing-parent))))

(defn parse-clj-string
  [parser character]
  (when (currently-parsing? parser :clj-string)
    (cond
      (= \\ (last (current-token-content parser)))
      (-> parser (read-to-buffer character))

      (= \" character)
      (-> parser (read-to-buffer character) add-current-buffer-to-last)

      :else
      (-> parser (read-to-buffer character)))))

(defn parse-colon
  [parser character]
  (when (= \: character)
    (-> parser (read-to-new-buffer :colon character)
               (emit-token! :pop-stack? true))))

(declare parse-newline)

(defn parse-comment
  [parser character]
  (when (currently-parsing? parser :comment)
    (if (= \newline character)
      (-> parser (emit-token! :pop-stack? true) (parse-newline character))
      (-> parser (read-to-buffer character)))))

(defn parse-cram-open
  [parser character]
  (when (= \{ character)
    (-> parser (read-to-new-buffer :cram-open character)
               (emit-token! :pop-stack? true))))

(defn parse-cram-close
  [parser character]
  (when (= \} character)
    (-> parser (read-to-new-buffer :cram-close character)
               (emit-token! :pop-stack? true)
               (new-buffer :duration))))

(defn parse-duration
  [p c]
  (when (currently-parsing? p :duration)
    (condp contains? c
      #{\space}
      (-> p (advance c))

      #{\newline}
      (-> p (parse-newline c))

      #{\0 \1 \2 \3 \4 \5 \6 \7 \8 \9}
      (-> p (start-parsing-note-length c))

      #{\|}
      (-> p (parse-barline c))

      #{\~}
      (-> p (parse-tie c))

      ; else
      (-> p discard-buffer (read-character! c)))))

(defn parse-equals
  [parser character]
  (when (= \= character)
    (-> parser (read-to-new-buffer :equals character)
               (emit-token! :pop-stack? true))))

(defn parse-event-seq-open
  [parser character]
  (when (= \[ character)
    (-> parser (read-to-new-buffer :event-seq-open character)
               (emit-token! :pop-stack? true))))

(defn parse-event-seq-close
  [parser character]
  (when (= \] character)
    (-> parser (read-to-new-buffer :event-seq-close character)
               (emit-token! :pop-stack? true))))

(defn parse-marker
  [parser character]
  (when (currently-parsing? parser :marker)
    (if ((set (str "abcdefghijklmnopqrstuvwxyz"
                   "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                   "0123456789_-"))
         character)
      (-> parser (read-to-buffer character))
      (-> parser (emit-token! :pop-stack? true)
                 (read-character! character)))))

(defn parse-name
  [parser character]
  (when (currently-parsing? parser :name)
    (if ((set (str "abcdefghijklmnopqrstuvwxyz"
                   "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                   "0123456789_-."))
         character)
      (-> parser (read-to-buffer character))
      (-> parser (emit-token! :pop-stack? true)
                 (read-character! character)))))

(defn parse-newline
  [parser character]
  (when (= \newline character)
    (-> parser (read-to-new-buffer :newline character)
               (emit-token! :pop-stack? true))))

(defn parse-nickname
  [parser character]
  (when (currently-parsing? parser :nickname)
    (let [buffer (current-token-content parser)]
      (cond
        (= \" character)
        (-> parser (emit-token! :pop-stack? true)
                   (advance character))

        ((set (str "abcdefghijklmnopqrstuvwxyz"
                   "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                   "0123456789_-"))
         character)
        (-> parser (read-to-buffer character))

        :else
        (unexpected-char-error parser character)))))

(defn parse-note
  [parser character]
  (when (currently-parsing? parser :note)
    (condp contains? character
      #{\+ \- \_}
      (-> parser (emit-token! :pop-stack? true)
          (start-parsing-accidentals character))

      #{\0 \1 \2 \3 \4 \5 \6 \7 \8 \9}
      (-> parser (emit-token! :pop-stack? true)
          (start-parsing-duration character))

      #{\~}
      (-> parser (emit-token! :pop-stack? true)
          (parse-tie character))

      ; else
      (-> parser (emit-token! :pop-stack? true)
          (read-character! character)))))

(defn parse-note-length
  [parser character]
  (when (currently-parsing? parser :note-length)
    (let [buffer (current-token-content parser)]
      (condp contains? character
        #{\0 \1 \2 \3 \4 \5 \6 \7 \8 \9}
        (-> parser (read-to-buffer character))

        #{\.}
        (if (re-matches #"(\d+\.)?\d+\.*" buffer)
          (-> parser (read-to-buffer character))
          (-> parser (unexpected-char-error character)))

        #{\m}
        (if (re-matches #"\d+" buffer)
          (-> parser (read-to-buffer character))
          (-> parser (unexpected-char-error character)))

        #{\s}
        (if (re-matches #"\d+m?" buffer)
          (-> parser (read-to-buffer character))
          (-> parser (unexpected-char-error character)))

        ; else
        (-> parser (emit-token! :pop-stack? true)
                   (start-parsing-duration character))))))

(defn parse-note-rest-or-name
  "Parse a character that could be part of:
   - a variable name
   - a note
   - a rest
   - an instrument call"
  [parser character]
  (when (currently-parsing? parser :note-rest-or-name)
    (let [buffer (current-token-content parser)
          char1  (first buffer)
          parse  #(-> %1 (rename-current-token %2)
                         (read-character! character))]
      (cond
        (and ((set "abcdefg") char1)
             ((conj (set "# \n+-_/~*'}]<>0123456789") :EOF) character))
        (-> parser (parse :note))

        (and (= \r char1)
             ((conj (set "# \n/~*}]<>0123456789") :EOF) character))
        (-> parser (parse :rest))

        ((set "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ") character)
        (-> parser (parse :name))

        :else
        (-> parser (unexpected-char-error character))))))

(defn parse-repeat
  [parser character]
  (let [buffer (current-token-content parser)]
    (when (currently-parsing? parser :repeat)
      (cond
        (#{\0 \1 \2 \3 \4 \5 \6 \7 \8 \9} character)
        (-> parser (read-to-buffer character))

        (re-matches #"\d+" buffer)
        (-> parser (emit-token! :pop-stack? true)
                   (read-character! character))

        (= \space character)
        (-> parser (advance character))

        (= \newline character)
        (-> parser (parse-newline character))

        :else
        (-> parser (unexpected-char-error character))))))

(defn parse-repeat-num
  [parser character]
  (let [buffer (current-token-content parser)
        digits (set "0123456789")]
    (when (currently-parsing? parser :repeat-num)
      (condp contains? character
        #{\0 \1 \2 \3 \4 \5 \6 \7 \8 \9}
        (-> parser (read-to-buffer character))

        #{\- \,}
        (if (contains? digits (last buffer))
          (-> parser (read-to-buffer character))
          (-> parser (unexpected-char-error character)))

        #{\space}
        (-> parser (advance character))

        #{\newline}
        (-> parser (parse-newline character))

        ; else
        (if (contains? digits (last buffer))
          (-> parser
              (emit-token! :pop-stack? true)
              (read-character! character))
          (-> parser (unexpected-char-error character)))))))


(defn parse-rest
  [parser character]
  (when (currently-parsing? parser :rest)
    (condp contains? character
      #{\space :EOF}
      (-> parser
          (advance character)
          (emit-token! :pop-stack? true))

      #{\0 \1 \2 \3 \4 \5 \6 \7 \8 \9}
      (-> parser
          (emit-token! :pop-stack? true)
          (start-parsing-duration character))

      ; else
      (-> parser
          (emit-token! :pop-stack? true)
          (read-character! character)))))

(defn parse-slash
  [parser character]
  (when (= \/ character)
    (-> parser (read-to-new-buffer :slash character)
               (emit-token! :pop-stack? true))))

(defn parse-tie
  [parser character]
  (when (= \~ character)
    (-> parser (read-to-new-buffer :tie character)
               (emit-token! :pop-stack? true))))

(defn parse-voice
  [parser character]
  (when (currently-parsing? parser :voice)
    (let [buffer (current-token-content parser)]
      (condp re-matches buffer
        #""
        (if (= \V character)
          (-> parser (read-to-buffer character))
          (-> parser (unexpected-char-error character)))

        #"V"
        (condp contains? character
          #{\0 \1 \2 \3 \4 \5 \6 \7 \8 \9}
          (-> parser (read-to-buffer character))

          (set "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
          (-> parser (rename-current-token :name)
                     (read-character! character))

          ; else
          (-> parser (unexpected-char-error character)))

        #"V\d+"
        (condp contains? character
          #{\0 \1 \2 \3 \4 \5 \6 \7 \8 \9}
          (-> parser (read-to-buffer character))

          #{\:}
          (-> parser (emit-token! :pop-stack? true)
                     (advance character)))))))

(defn parse-octave-change
  [parser character]
  (when (currently-parsing? parser :octave-change)
    (let [buffer (current-token-content parser)]
      (condp re-matches buffer
        #""
        (condp contains? character
          #{\o}
          (-> parser (read-to-buffer character))

          #{\< \>}
          (-> parser (read-to-buffer character) (emit-token! :pop-stack? true))

          ; else
          (-> parser (unexpected-char-error character)))

        #"o"
        (condp contains? character
          #{\- \0 \1 \2 \3 \4 \5 \6 \7 \8 \9}
          (-> parser (read-to-buffer character))

          (set "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
          (-> parser (rename-current-token :name)
                     (read-character! character))

          ; else
          (-> parser (unexpected-char-error character)))

        #"o-?\d+"
        (condp contains? character
          #{\0 \1 \2 \3 \4 \5 \6 \7 \8 \9}
          (-> parser (read-to-buffer character))

          #{\space :EOF}
          (-> parser (emit-token! :pop-stack? true) (advance character))

          #{\newline}
          (-> parser (emit-token! :pop-stack? true) (parse-newline character))

          ; else
          (-> parser (unexpected-char-error character)))))))

(defn read-character!
  "Reads one character `c` and updates parser `p`.

   Puts tokens on (:tokens-ch p) as they are parsed."
  [p c]
  (try
    (or (ensure-parsing p)
        (ignore-carriage-return p c)
        (parse-comment p c)
        (parse-clj-sexp p c)
        (parse-clj-string p c)
        (parse-clj-char p c)
        (parse-note p c)
        (parse-rest p c)
        (parse-name p c)
        (parse-nickname p c)
        (parse-voice p c)
        (parse-octave-change p c)
        (parse-marker p c)
        (parse-at-marker p c)
        (parse-note-rest-or-name p c)
        (parse-duration p c)
        (parse-note-length p c)
        (parse-accidentals p c)
        (parse-repeat p c)
        (parse-repeat-num p c)
        (parse-slash p c)
        (parse-colon p c)
        (parse-barline p c)
        (parse-equals p c)
        (parse-newline p c)
        (parse-cram-open p c)
        (parse-cram-close p c)
        (parse-event-seq-open p c)
        (parse-event-seq-close p c)
        (start-parsing-clj-sexp p c)
        (start-parsing-comment p c)
        (start-parsing-voice p c)
        (start-parsing-octave-change p c)
        (start-parsing-marker p c)
        (start-parsing-at-marker p c)
        (start-parsing-note-rest-or-name p c)
        (start-parsing-nickname p c)
        (start-parsing-repeat p c)
        (start-parsing-repeat-num p c)
        (finish-parsing p c)
        (skip-whitespace p c)
        (unexpected-char-error p c))
    (catch Throwable e
      (emit-error! p e))))
