(ns alda.lisp.model.event
  (:require [alda.lisp.model.offset :refer (absolute-offset)]))

(defmulti update-score*
  "See `update-score`."
  (fn [score event]
    (cond
      (or (= :nil event)
          (nil? event)
          (var? event))   :nil
      (sequential? event) :event-sequence
      :else               (:event-type event))))

(defmethod update-score* :default
  [_ event]
  (throw (Exception. (str "Invalid event: " (pr-str event)))))

(defmethod update-score* :nil
  [score _]
  ; pass score through unchanged
  ; e.g. for side-effecting inline Clojure code
  score)

(defn update-master-tempo-values
  "In an Alda score, each part has its own tempo and it can differ from the
   other parts' tempos.

   Nonetheless, we need to maintain a notion of a single \"master\" tempo in
   order to support features like MIDI export.

   A score has exactly one part whose role is the tempo \"master\".

   The master tempo is derived from local tempo attribute changes for this part,
   as well as global tempo attribute changes."
  [score]
  (let [master-part-tempo (->> score
                               :instruments
                               vals
                               (filter #(= :master (:tempo/role %)))
                               first
                               :tempo/values)
        global-tempo      (reduce-kv
                            ;; `attr-changes` is a map of attribute to vector of
                            ;; attribute values. It's a vector because
                            ;; technically, the attribute can be set multiple
                            ;; times at the same point in time, which is useful
                            ;; sometimes because the effect can be cumulative.
                            ;;
                            ;; In the case of tempo, it's last writer wins, i.e.
                            ;; (tempo! 120) (tempo! 125) means the tempo is 125
                            ;; at that point in time and the 120 is thrown out.
                            (fn [m offset-ms attr-changes]
                              (if-let [tempo-vals (:tempo attr-changes)]
                                (assoc m offset-ms (last tempo-vals))
                                m))
                            {}
                            (:global-attributes score))
        ;; At this point, we have a map of offset (ms) to tempo (bpm).
        ;; In the case where no initial tempo is specified, we default to 120.
        tempo-values      (merge {0 120} master-part-tempo global-tempo)]
    (assoc score :tempo/values tempo-values)))

(defn update-score
  "Events in Alda are represented as maps containing, at the minimum, a value
   for :event-type to serve as a unique identifier (by convention, a keyword)
   to be used as a dispatch value.

   An Alda score S-expression simply reduces `update-score` over all of the
   score's events, with the initial score state as the initial value to be
   reduced.

   Lists/vectors are a special case -- they are reduced internally and treated
   as a single 'event sequence'."
  [score event]
  (-> score
      (update-score* event)
      update-master-tempo-values))

; utility fns

(defn add-event
  [{:keys [instruments events markers] :as score}
   {:keys [instrument offset] :as event}]
  (update score :events conj (update event :offset #(absolute-offset % score))))

(defn add-events
  [score events]
  (reduce add-event score events))

