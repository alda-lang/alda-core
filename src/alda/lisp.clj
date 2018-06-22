(ns alda.lisp
  "alda.parser transforms Alda code into Clojure code, which can then be
   evaluated with the help of this namespace."
  (:require [potemkin.namespaces :refer (import-vars)]
            [alda.util           :as    util]))

; sets log level to TIMBRE_LEVEL (if set) or :warn
(util/set-log-level!)

(defn- import-all-vars
  "Imports all public vars from a namespace into the alda.lisp namespace."
  [ns]
  (eval (list `import-vars (cons ns (keys (ns-publics ns))))))

(def ^:private namespaces
  '[alda.lisp.attributes
    alda.lisp.code
    alda.lisp.events
    alda.lisp.events.barline
    alda.lisp.events.chord
    alda.lisp.events.cram
    alda.lisp.events.note
    alda.lisp.events.rest
    alda.lisp.events.sequence
    alda.lisp.events.variable
    alda.lisp.events.voice
    alda.lisp.instruments.midi
    alda.lisp.model.attribute
    alda.lisp.model.duration
    alda.lisp.model.event
    alda.lisp.model.global-attribute
    alda.lisp.model.marker
    alda.lisp.model.offset
    alda.lisp.model.pitch
    alda.lisp.score
    alda.lisp.score.part])

(doseq [ns namespaces]
  (require ns)
  (import-all-vars ns))

;; This can be used to determine whether alda.lisp has been loaded into an
;; environment where it is needed, e.g. parsing a score that includes inline
;; Clojure expressions like (tempo! 80)
(declare ALDA-LISP-LOADED)

