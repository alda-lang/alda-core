(ns alda.lisp.events.barline
  (:require [alda.lisp.model.event :refer (update-score*)]))

(defmethod update-score* :barline
  [score _]
  "Barlines, at least currently, do nothing when evaluated in alda.lisp."
  score)

