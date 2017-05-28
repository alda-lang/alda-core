(ns alda.lisp.code
  (:require [alda.parser :refer (parse-input)]))

(defn alda-code
  "Attempts to parse a string of text within the context of the current score;
   if the code parses successfully, the result is one or more events that are
   spliced into the score."
  [code]
  (parse-input code :output :events))
