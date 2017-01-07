(ns alda.import
  "alda.import transforms other music files into Alda scores, which can then be stored
   or played. Currently we support importing MIDI files."
  (:require [potemkin.namespaces :refer (import-vars)]
            [alda.util           :as    util]))

; sets log level to TIMBRE_LEVEL (if set) or :warn
(util/set-log-level!)

(defn- import-all-vars
  "Imports all public vars from a namespace into the alda.import namespace."
  [ns]
  (eval (list `import-vars (cons ns (keys (ns-publics ns))))))

(def ^:private namespaces
  '[alda.import.midi])

(doseq [ns namespaces]
  (require ns))
