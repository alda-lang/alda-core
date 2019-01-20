(ns alda.lisp.instruments.midi
  (:require [alda.lisp.model.instrument :as inst]))

; NOTE: For the time being, some of these instruments have non-prefixed names
;       like "piano" and "trumpet" as aliases. I eventually want to re-map
;       those names to more realistic-sounding, sampled instruments instead of
;       MIDI. Once that happens, we'll need to remove the aliases.

; reference: http://www.jimmenard.com/midi_ref.html#General_MIDI

(def non-percussion-instruments
  "A list of the 128 instruments in the General MIDI spec, in order.

   Each instrument is represented as a vector where the first item is the
   \"official\"/preferred name to be used in the Alda instrument list, and is
   followed by recognized aliases."
  [
   ;; 1-8: piano
   ["midi-acoustic-grand-piano" "midi-piano" "piano"]
   ["midi-bright-acoustic-piano"]
   ["midi-electric-grand-piano"]
   ["midi-honky-tonk-piano"]
   ["midi-electric-piano-1"]
   ["midi-electric-piano-2"]
   ["midi-harpsichord" "harpsichord"]
   ["midi-clavi" "midi-clavinet" "clavinet"]
   ;; 9-16: chromatic percussion
   ["midi-celesta" "celesta" "celeste" "midi-celeste"]
   ["midi-glockenspiel" "glockenspiel"]
   ["midi-music-box" "music-box"]
   ["midi-vibraphone" "vibraphone" "vibes" "midi-vibes"]
   ["midi-marimba" "marimba"]
   ["midi-xylophone" "xylophone"]
   ["midi-tubular-bells" "tubular-bells"]
   ["midi-dulcimer" "dulcimer"]
   ;; 17-24: organ
   ["midi-drawbar-organ"]
   ["midi-percussive-organ"]
   ["midi-rock-organ"]
   ["midi-church-organ" "organ"]
   ["midi-reed-organ"]
   ["midi-accordion" "accordion"]
   ["midi-harmonica" "harmonica"]
   ["midi-tango-accordion"]
   ;; 25-32: guitar
   ["midi-acoustic-guitar-nylon" "midi-acoustic-guitar" "acoustic-guitar" "guitar"]
   ["midi-acoustic-guitar-steel"]
   ["midi-electric-guitar-jazz"]
   ["midi-electric-guitar-clean" "electric-guitar-clean"]
   ["midi-electric-guitar-palm-muted"]
   ["midi-electric-guitar-overdrive" "electric-guitar-overdrive"]
   ["midi-electric-guitar-distorted" "electric-guitar-distorted"]
   ["midi-electric-guitar-harmonics" "electric-guitar-harmonics"]
   ;; 33-40: bass
   ["midi-acoustic-bass" "acoustic-bass" "upright-bass"]
   ["midi-electric-bass-finger" "electric-bass-finger" "electric-bass"]
   ["midi-electric-bass-pick" "electric-bass-pick"]
   ["midi-fretless-bass" "fretless-bass"]
   ["midi-bass-slap"]
   ["midi-bass-pop"]
   ["midi-synth-bass-1"]
   ["midi-synth-bass-2"]
   ;; 41-48: strings
   ["midi-violin" "violin"]
   ["midi-viola" "viola"]
   ["midi-cello" "cello"]
   ["midi-contrabass" "string-bass" "arco-bass" "double-bass" "contrabass"
    "midi-string-bass" "midi-arco-bass" "midi-double-bass"]
   ["midi-tremolo-strings"]
   ["midi-pizzicato-strings"]
   ["midi-orchestral-harp" "harp" "orchestral-harp" "midi-harp"]
   ;; no idea why this is in strings, but ok! ¯\_(ツ)_/¯
   ["midi-timpani" "timpani"]
   ;; 49-56: ensemble
   ["midi-string-ensemble-1"]
   ["midi-string-ensemble-2"]
   ["midi-synth-strings-1"]
   ["midi-synth-strings-2"]
   ["midi-choir-aahs"]
   ["midi-voice-oohs"]
   ["midi-synth-voice"]
   ["midi-orchestra-hit"]
   ;; 57-64: brass
   ["midi-trumpet" "trumpet"]
   ["midi-trombone" "trombone"]
   ["midi-tuba" "tuba"]
   ["midi-muted-trumpet"]
   ["midi-french-horn" "french-horn"]
   ["midi-brass-section"]
   ["midi-synth-brass-1"]
   ["midi-synth-brass-2"]
   ;; 65-72: reed
   ["midi-soprano-saxophone" "midi-soprano-sax" "soprano-saxophone"
    "soprano-sax"]
   ["midi-alto-saxophone" "midi-alto-sax" "alto-saxophone" "alto-sax"]
   ["midi-tenor-saxophone" "midi-tenor-sax" "tenor-saxophone" "tenor-sax"]
   ["midi-baritone-saxophone" "midi-baritone-sax" "midi-bari-sax"
    "baritone-saxophone" "baritone-sax" "bari-sax"]
   ["midi-oboe" "oboe"]
   ["midi-english-horn" "english-horn"]
   ["midi-bassoon" "bassoon"]
   ["midi-clarinet" "clarinet"]
   ;; 73-80: pipe
   ["midi-piccolo" "piccolo"]
   ["midi-flute" "flute"]
   ["midi-recorder" "recorder"]
   ["midi-pan-flute" "pan-flute"]
   ["midi-bottle" "bottle"]
   ["midi-shakuhachi" "shakuhachi"]
   ["midi-whistle" "whistle"]
   ["midi-ocarina" "ocarina"]
   ;; 81-88: synth lead
   ["midi-square-lead" "square" "square-wave" "square-lead" "midi-square"
    "midi-square-wave"]
   ["midi-saw-wave" "sawtooth" "saw-wave" "saw-lead" "midi-sawtooth"
    "midi-saw-lead"]
   ["midi-calliope-lead" "calliope-lead" "calliope" "midi-calliope"]
   ["midi-chiffer-lead" "chiffer-lead" "chiffer" "chiff" "midi-chiffer"
    "midi-chiff"]
   ["midi-charang" "charang"]
   ["midi-solo-vox"]
   ["midi-fifths" "midi-sawtooth-fifths"]
   ["midi-bass-and-lead" "midi-bass+lead"]
   ;; 89-96: synth pad
   ["midi-synth-pad-new-age" "midi-pad-new-age" "midi-new-age-pad"]
   ["midi-synth-pad-warm" "midi-pad-warm" "midi-warm-pad"]
   ["midi-synth-pad-polysynth" "midi-pad-polysynth" "midi-polysynth-pad"]
   ["midi-synth-pad-choir" "midi-pad-choir" "midi-choir-pad"]
   ["midi-synth-pad-bowed" "midi-pad-bowed" "midi-bowed-pad"
    "midi-pad-bowed-glass" "midi-bowed-glass-pad"]
   ["midi-synth-pad-metallic" "midi-pad-metallic" "midi-metallic-pad"
    "midi-pad-metal" "midi-metal-pad"]
   ["midi-synth-pad-halo" "midi-pad-halo" "midi-halo-pad"]
   ["midi-synth-pad-sweep" "midi-pad-sweep" "midi-sweep-pad"]
   ;; 97-104: synth effects
   ["midi-fx-rain" "midi-fx-ice-rain" "midi-rain" "midi-ice-rain"]
   ["midi-fx-soundtrack" "midi-soundtrack"]
   ["midi-fx-crystal" "midi-crystal"]
   ["midi-fx-atmosphere" "midi-atmosphere"]
   ["midi-fx-brightness" "midi-brightness"]
   ["midi-fx-goblins" "midi-fx-goblin" "midi-goblins" "midi-goblin"]
   ["midi-fx-echoes" "midi-fx-echo-drops" "midi-echoes" "midi-echo-drops"]
   ["midi-fx-sci-fi" "midi-sci-fi"]
   ;; 105-112: "ethnic" (sigh)
   ["midi-sitar" "sitar"]
   ["midi-banjo" "banjo"]
   ["midi-shamisen" "shamisen"]
   ["midi-koto" "koto"]
   ["midi-kalimba" "kalimba"]
   ["midi-bagpipes" "bagpipes"]
   ["midi-fiddle"]
   ["midi-shehnai" "shehnai" "shahnai" "shenai" "shanai" "midi-shahnai"
    "midi-shenai" "midi-shanai"]
   ;; 113-120: percussive
   ["midi-tinkle-bell" "midi-tinker-bell"]
   ["midi-agogo"]
   ["midi-steel-drums" "midi-steel-drum" "steel-drums" "steel-drum"]
   ["midi-woodblock"]
   ["midi-taiko-drum"]
   ["midi-melodic-tom"]
   ["midi-synth-drum"]
   ["midi-reverse-cymbal"]
   ;; 121-128: sound effects
   ["midi-guitar-fret-noise"]
   ["midi-breath-noise"]
   ["midi-seashore"]
   ["midi-bird-tweet"]
   ["midi-telephone-ring"]
   ["midi-helicopter"]
   ["midi-applause"]
   ["midi-gunshot" "midi-gun-shot"]])

(def percussion-instruments
  "There is only one percussion instrument: midi-percussion

   This is a definition of that instrument that has the same shape as
   `non-percussion-instruments`."
  [["midi-percussion" "percussion"]])

(def instruments
  (concat non-percussion-instruments percussion-instruments))

(doseq [[i [preferred-name & aliases :as names]]
        (map-indexed vector non-percussion-instruments)

        name
        names]
  (inst/define-instrument!
    name
    preferred-name
    {}
    {:type :midi, :patch (inc i)}))

(doseq [[preferred-name & aliases :as names] percussion-instruments
        name names]
  (inst/define-instrument!
    name
    preferred-name
    {}
    {:type :midi, :percussion? true}))

