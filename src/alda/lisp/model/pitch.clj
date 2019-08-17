(ns alda.lisp.model.pitch)

(def ^:private intervals
  {:c 0, :d 2, :e 4, :f 5, :g 7, :a 9, :b 11})

(defn- midi-note-number
  "Given a letter and an octave, returns the MIDI note number.
   e.g. :c, 4  =>  60"
  [letter octave]
  (+ (intervals letter) (* octave 12) 12))

(defn midi->hz
  "Converts a MIDI note number to the note's frequency in Hz."
  [ref-pitch note]
  (* ref-pitch (Math/pow 2.0 (/ (- note 69.0) 12.0))))

(defn- apply-key
  "Modifies the accidentals on notes to fit the key signature.

   If there are no accidentals and this letter is in the signature, return the
   letter's signature accidentals, otherwise return existing accidentals."
  [signature letter accidentals]
  (if (empty? accidentals)
    (get signature letter)
    accidentals))

(defn determine-midi-note
  "Determines the MIDI note number of a note, within the context of an
   instrument's octave and key signature."
  [{:keys [letter accidentals midi-note]} octave key-sig transpose]
  (+ transpose
     (if midi-note
       midi-note
       (reduce (fn [number accidental]
                 (case accidental
                   :flat    (dec number)
                   :sharp   (inc number)
                   :natural (identity number)))
               (midi-note-number letter octave)
               (apply-key key-sig letter accidentals)))))

(defn pitch
  [letter & accidentals]
  {:letter      letter
   :accidentals (or accidentals [])})

(defn midi-note
  [note-number]
  {:midi-note note-number})
