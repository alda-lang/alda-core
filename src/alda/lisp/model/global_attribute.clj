(ns alda.lisp.model.global-attribute
  (:require [alda.lisp.events          :refer (set-attribute)]
            [alda.lisp.model.attribute :refer (apply-attribute)]
            [alda.lisp.model.event     :refer (update-score update-score*)]
            [alda.lisp.model.offset    :refer (absolute-offset
                                               instruments-all-at-same-offset)]
            [alda.lisp.score.util      :refer (update-instruments)]
            [taoensso.timbre           :as    log]))

(defn- round-to-precision
  "Round a double to the given precision (number of significant digits)"
  [precision d]
  (let [factor (Math/pow 10 precision)]
    (/ (Math/round (* d factor)) factor)))

(defn- fudge
  "There are some very subtle timing inaccuracies (imperceptible to the human
   ear) related to the way we're doing math with floating point numbers. As a
   result, we can sometimes inadvertently place a global attribute a fraction of
   a millisecond too late, and so the attribute change might not be picked up
   until it's too late.

   Alda v2 seems to be immune to this problem for some reason. Floating math is
   weird, and it's even harder to understand the differences between the math
   stacks in different programming language runtimes (in this case, Clojure vs.
   Go). I don't have anywhere near enough free time to figure out for sure
   what's happening, so as a workaround, we're going to fudge the numbers a bit
   in Alda v1: we're going to round off the offsets for recorded global
   attribute changes to the nearest ten-thousandth of a millisecond.

   e.g. instead of recording a global attribute change at 250.00000381469727 ms,
   we'll record it at 250.0000 ms."
  [number]
  (round-to-precision 4 number))

(defmethod update-score* :global-attribute-change
  [score {:keys [attr val] :as event}]
  (if-let [offset (instruments-all-at-same-offset score)]
    (let [abs-offset (fudge (absolute-offset offset score))]
      (log/debugf "Set global attribute %s %s at offset %d."
                  attr val (int abs-offset))
      (-> score
          (update-in [:global-attributes abs-offset attr] (fnil conj []) val)
          (update-score (set-attribute attr val))))
    (throw (Exception.
             (str "Can't set global attribute " attr " to " val " - offset "
                  "unclear. There are multiple instruments active with "
                  "different time offsets.")))))

(defn- global-attribute-changes
  "Determines the attribute changes to apply to an instrument, based on the
   attribute changes established in the score (global attributes) and the
   instrument's :last- and :current-offset.

   Returns a map of updated attributes to their new values.

   Each 'value' is actually a vector of values, the length of which depends on
   the number of times this attribute was changed at that point in time. For
   example, if the octave is incremented a bunch of times, then the 'value'
   here will be something like [:up :up :up], whereas a single octave change
   will just be [:up]. `update-score` below will apply the updates
   sequentially."
  [{:keys [global-attributes] :as score}
   {:keys [current-offset last-offset] :as inst}]
  (let [[last current] (map #(fudge (absolute-offset % score))
                            [last-offset current-offset])
        [start end]    (if (<= last current) [last current] [0 current])]
    (->> global-attributes
         (drop-while #(<= (key %) start))
         (take-while #(<= (key %) end))
         (map val)
         (apply merge))))

(defmethod update-score* :apply-global-attributes
  [{:keys [global-attributes instruments current-instruments]
    :as score} _]
  (update-instruments score
    (fn [{:keys [id current-offset last-offset] :as inst}]
      (if (contains? current-instruments id)
        (let [attr-changes (global-attribute-changes score inst)]
          (reduce (fn [inst [attr vals]]
                    (reduce #(apply-attribute score %1 attr %2)
                            inst
                            vals))
                  inst
                  attr-changes))
        inst))))

