(ns alda.lisp.model.instrument)

(def ^:dynamic *stock-instruments* {})

(defn define-instrument!
  [alias stock-name initial-vals config]
  (alter-var-root #'*stock-instruments*
                  assoc
                  alias
                  {:initial-vals (merge initial-vals
                                        {:stock stock-name
                                         :config config})}))

(defmacro definstrument
  "Defines a stock instrument."
  [inst-name & things]
  (let [{:keys [aliases initial-vals config] :as opts}
        (if (string? (first things)) (rest things) things)
        inst-aliases (vec (cons (str inst-name) (or aliases [])))
        initial-vals (or initial-vals {})]
    `(doseq [alias# ~inst-aliases]
       (define-instrument!
         alias#
         ~(str inst-name)
         ~initial-vals
         ~config))))
