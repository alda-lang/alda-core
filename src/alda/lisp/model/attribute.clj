(ns alda.lisp.model.attribute
  (:require [taoensso.timbre        :as    log]
            [alda.lisp.model.event  :refer (update-score*)]
            [alda.lisp.model.offset :refer (absolute-offset)]
            [alda.lisp.score.util   :refer (update-instruments)]))

; This is a map of keywords to Attribute records.
; When an attribute change event occurs in a score, we look up its attribute
; (a keyword) in this map and use the data about this name to update the
; appropriate attribute of the current instruments.
;
; This map gets filled in with Alda's various attributes via the `defattribute`
; macro in alda.lisp.attributes.
(def ^:dynamic *attribute-table* {})

(defn get-attr
  "Given a keyword representing an attribute (which could be an alias, e.g.
   :quant for :quantization), returns the attribute map, which includes the
   attribute's keyword name (e.g. :quantization) and its transform function.

   The transform function is a higher-order function which takes a new,
   user-friendly value (e.g. 100 instead of 1.0) and returns the function to
   apply to an instrument's existing value to update it to the new value.
   (See alda.lisp.attributes for examples of transform functions.)

   Throws an exception if the argument supplied is not a valid keyword name
   or alias for an existing attribute."
  [kw]
  (if-let [attr (*attribute-table* kw)]
    attr
    (throw (Exception. (str kw " is not a valid attribute.")))))

(defn log-attribute-change
  [{:keys [id] :as inst} attr old-val new-val]
  (log/debugf "%s %s changed from %s to %s." id (str attr) old-val new-val))

(defn- track-tempo-changes
  "The tempo attribute is a special case because it has an impact on the MIDI
   sequence that an Alda score generates.

   Usually, we only need to worry about a part's current attribute values.
   However, for tempo, we need to have a comprehensive history of each part's
   tempo and every time it changed during the score."
  [inst attr-name attr-val score]
  (if (= :tempo attr-name)
    (let [offset (-> inst :current-offset (absolute-offset score))]
      (assoc-in inst [:tempo/values offset] attr-val))
    inst))

(defn apply-attribute
  "Given an instrument map, a keyword representing an attribute, and a value,
   returns the updated instrument with that attribute update applied."
  [{:keys [beats-tally] :as score} inst attr val]
  (let [{:keys [transform-fn kw-name]} (get-attr attr)
        old-val (kw-name inst)
        new-val ((transform-fn val) old-val)]
    (when (and (not= old-val new-val) (not beats-tally))
      (log-attribute-change inst kw-name old-val new-val))
    (-> inst
        (assoc kw-name new-val)
        (track-tempo-changes kw-name new-val score))))

(defmethod update-score* :attribute-change
  [{:keys [current-instruments] :as score}
   {:keys [attr val] :as attr-change}]
  (update-instruments score
    (fn [{:keys [id] :as inst}]
      (if (contains? current-instruments id)
        (apply-attribute score inst attr val)
        inst))))

