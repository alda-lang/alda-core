# alda-core development guide

## Components

* [alda.lisp](#aldalisp)
* [alda.parser](#aldaparser)

### alda.lisp

Alda is implemented as a [domain-specific language (DSL)](https://en.wikipedia.org/wiki/Domain-specific_language) that can be used to construct a musical score:

```clojure
(score
  (part {:names ["piano"]}
    (note
      (pitch :c)
      (duration (note-length 8)))
    (note
      (pitch :e))
    (note
      (pitch :g))
    (chord
      (note
        (pitch :c)
        (duration (note-length 1)))
      (note
        (pitch :f))
      (note
        (pitch :a)))))
```

When you evaluate a score [S-expression](https://en.wikipedia.org/wiki/S-expression) like the one above, the result is a map of score information, which provides all of the data that Alda's audio component needs in order to play your score.

```clojure
{:chord-mode false,
 :current-instruments #{"piano-zerpD"},
 :events
 #{{:offset 750.0,
    :instrument "piano-zerpD",
    :volume 1.0,
    :track-volume 0.7874015748031497,
    :panning 0.5,
    :midi-note 60,
    :pitch 261.6255653005986,
    :duration 1800.0,
    :voice nil}
   {:offset 750.0,
    :instrument "piano-zerpD",
    :volume 1.0,
    :track-volume 0.7874015748031497,
    :panning 0.5,
    :midi-note 69,
    :pitch 440.0,
    :duration 1800.0,
    :voice nil}
   {:offset 500.0,
    :instrument "piano-zerpD",
    :volume 1.0,
    :track-volume 0.7874015748031497,
    :panning 0.5,
    :midi-note 67,
    :pitch 391.99543598174927,
    :duration 225.0,
    :voice nil}
   {:offset 750.0,
    :instrument "piano-zerpD",
    :volume 1.0,
    :track-volume 0.7874015748031497,
    :panning 0.5,
    :midi-note 65,
    :pitch 349.2282314330039,
    :duration 1800.0,
    :voice nil}
   {:offset 250.0,
    :instrument "piano-zerpD",
    :volume 1.0,
    :track-volume 0.7874015748031497,
    :panning 0.5,
    :midi-note 64,
    :pitch 329.6275569128699,
    :duration 225.0,
    :voice nil}
   {:offset 0,
    :instrument "piano-zerpD",
    :volume 1.0,
    :track-volume 0.7874015748031497,
    :panning 0.5,
    :midi-note 60,
    :pitch 261.6255653005986,
    :duration 225.0,
    :voice nil}},
 :beats-tally nil,
 :instruments
 {"piano-zerpD"
  {:octave 4,
   :current-offset {:offset 2750.0},
   :key-signature {},
   :config {:type :midi, :patch 1},
   :duration 4.0,
   :min-duration nil,
   :volume 1.0,
   :last-offset {:offset 750.0},
   :id "piano-zerpD",
   :quantization 0.9,
   :duration-inside-cram nil,
   :tempo 120,
   :panning 0.5,
   :current-marker :start,
   :time-scaling 1,
   :stock "midi-acoustic-grand-piano",
   :track-volume 0.7874015748031497}},
 :markers {:start 0},
 :cram-level 0,
 :global-attributes {},
 :nicknames {},
 :beats-tally-default nil}
```

There are a lot of different values in this map, most of which the sound engine doesn't care about. The sound engine is mainly concerned with these 2 keys:

* **:events** -- a set of note events
* **:instruments** -- a map of randomly-generated ids to all of the information that Alda has about an instrument, *at the point where the score ends*.

A note event contains information such as the pitch, MIDI note and duration of a note, which instrument instance is playing the note, and what its offset is relative to the beginning of the score (i.e., where the note is in the score)

The sound engine decides how to play a note by looking up its instrument ID (which is defined on each event map) in the `:instruments` map. Each instrument has a `:config`, which tells the sound engine things like whether or not it's a MIDI instrument, and if it is a MIDI instrument, which General MIDI patch to use.

The remaining keys in the map are used by the score evaluation process to keep track of the state of the score. This includes information like which instruments' parts the composer is currently writing, how far into the score each instrument is (i.e. when that instrument's next note should come in), and the current values of attributes like volume, octave, and panning for each instrument used in the score.

Because `alda.lisp` is a Clojure DSL, it's possible to use it to build scores within a Clojure program, as an alternative to using Alda syntax:

```clojure
(ns my-clj-project.core
  (:require [alda.lisp :refer :all]))

(score
  (part "piano"
    (note (pitch :c) (duration (note-length 8)))
    (note (pitch :d))
    (note (pitch :e))
    (note (pitch :f))
    (note (pitch :g))
    (note (pitch :a))
    (note (pitch :b))
    (octave :up)
    (note (pitch :c))))
```

Alda's parser also uses the `alda.lisp` implementation to construct scores from
Alda code.

### alda.parser

#### The parsing pipeline

Alda parses a score in several stages:

- [Tokenize](https://github.com/alda-lang/alda/blob/master/src/alda/parser/tokenize.clj) the input.
- [Parse events](https://github.com/alda-lang/alda/blob/master/src/alda/parser/parse_events.clj) from the sequence of tokens.
- [Aggregate events](https://github.com/alda-lang/alda/blob/master/src/alda/parser/aggregate_events.clj) from the sequence of tokens.
- [Build a score](https://github.com/alda-lang/alda-core/blob/master/src/alda/lisp/score.clj) by starting from a new (empty) score and applying the events in order.

For optimal performance, the Alda parser performs the steps of this pipeline
asynchronously. As soon as the first token is parsed from the input string, it
goes on a [core.async](http://www.braveclojure.com/core-async) channel and the
tokenizing continues while the next stage of the parser begins to consume the
tokens from the channel and parse events from them. This means that we can start
to build a score almost instantly, without having to wait for the rest of the
parsing pipeline to finish.

There are two convenience functions in the `alda.parser` namespace for working
with the streams of tokens/events resulting from each step of the pipeline:

- `print-stream` prints items asynchronously as they are received.
- `stream-seq` produces a [lazy sequence](https://clojure.org/reference/sequences) of items received from the stream.

Using `print-stream` in a Clojure REPL, we can get an idea of what results from
each stage of the parsing pipeline:

```clojure
;; Wherever you see #object[...ManyToManyChannel...] below, that is the return
;; value of each stage of the pipeline: a channel from which events can be received
;; at the next stage of the pipeline. This is what allows us to thread each stage
;; into the next via the threading (->) operator.
;;
;; Note that because the printing (via print-stream) is happening asynchronously,
;; the REPL often prints the return value before all of the events are done being
;; printed.

;; STAGE 1: input => tokens
alda.parser=> (-> "piano: c8 e g > c4/e" tokenize print-stream)
[:name [1 1] "piano"]
[:colon [1 6] ":"]
[:note [1 8] "c"]
[:note-length [1 9] "8"]
[:note [1 11] "e"]
[:note [1 13] "g"]
[:octave-change [1 15] ">"]
[:note [1 17] "c"]
[:note-length [1 18] "4"]
[:slash [1 19] "/"]
[:note [1 20] "e"]
[:EOF [1 21]]
#object[clojure.core.async.impl.channels.ManyToManyChannel 0x6e85ec7b "clojure.core.async.impl.channels.ManyToManyChannel@6e85ec7b"]

;; STAGE 2: tokens => individual events
alda.parser=> (-> "piano: c8 e g > c4/e" tokenize parse-events print-stream)
#object[clojure.core.async.impl.channels.ManyToManyChannel 0x51a59f63 "clojure.core.async.impl.channels.ManyToManyChannel@51a59f63"]
{:event-type :part, :instrument-call {:names ["piano"]}, :events nil}
{:event-type :note, :letter :c, :accidentals [], :beats 0.5, :ms 0, :slur? nil}
{:event-type :note, :letter :e, :accidentals [], :beats nil, :ms nil, :slur? nil}
{:event-type :note, :letter :g, :accidentals [], :beats nil, :ms nil, :slur? nil}
{:event-type :attribute-change, :attr :octave, :val :up}
{:event-type :note, :letter :c, :accidentals [], :beats 1.0, :ms 0, :slur? nil}
{:event-type :note, :letter :e, :accidentals [], :beats nil, :ms nil, :slur? nil, :chord? true}
:EOF

;; STAGE 3: individual events => aggregated events
;; (e.g. notes => chords)
alda.parser=> (-> "piano: c8 e g > c4/e" tokenize parse-events aggregate-events print-stream)
#object[clojure.core.async.impl.channels.ManyToManyChannel 0x5bd8f56f "clojure.core.async.impl.channels.ManyToManyChannel@5bd8f56f"]
{:event-type :part, :instrument-call {:names ["piano"]}, :events nil}
{:event-type :note, :letter :c, :accidentals [], :beats 0.5, :ms 0, :slur? nil}
{:event-type :note, :letter :e, :accidentals [], :beats nil, :ms nil, :slur? nil}
{:event-type :note, :letter :g, :accidentals [], :beats nil, :ms nil, :slur? nil}
{:event-type :attribute-change, :attr :octave, :val :up}
{:event-type :chord, :events ({:event-type :note, :letter :c, :accidentals [], :beats 1.0, :ms 0, :slur? nil} {:event-type :note, :letter :e, :accidentals [], :beats nil, :ms nil, :slur? nil})}

;; STAGE 4: events => score
;; note that this only returns a single value on the stream, the final score
alda.parser=> (-> "piano: c8 e g > c4/e" tokenize parse-events aggregate-events build-score print-stream)
#object[clojure.core.async.impl.channels.ManyToManyChannel 0x6bbc10cb "clojure.core.async.impl.channels.ManyToManyChannel@6bbc10cb"]
{:chord-mode false, :current-instruments #{"piano-57rju"}, :events #{#alda.lisp.model.records.Note{:offset 250.0, :instrument "piano-57rju", :volume 1.0, :track-volume 0.7874015748031497, :panning 0.5, :midi-note 64, :pitch 329.6275569128699, :duration 225.0, :voice nil} #alda.lisp.model.records.Note{:offset 500.0, :instrument "piano-57rju", :volume 1.0, :track-volume 0.7874015748031497, :panning 0.5, :midi-note 67, :pitch 391.99543598174927, :duration 225.0, :voice nil} #alda.lisp.model.records.Note{:offset 750.0, :instrument "piano-57rju", :volume 1.0, :track-volume 0.7874015748031497, :panning 0.5, :midi-note 72, :pitch 523.2511306011972, :duration 450.0, :voice nil} #alda.lisp.model.records.Note{:offset 750.0, :instrument "piano-57rju", :volume 1.0, :track-volume 0.7874015748031497, :panning 0.5, :midi-note 76, :pitch 659.2551138257398, :duration 450.0, :voice nil} #alda.lisp.model.records.Note{:offset 0, :instrument "piano-57rju", :volume 1.0, :track-volume 0.7874015748031497, :panning 0.5, :midi-note 60, :pitch 261.6255653005986, :duration 225.0, :voice nil}}, :beats-tally nil, :instruments {"piano-57rju" {:octave 5, :current-offset #alda.lisp.model.records.AbsoluteOffset{:offset 1250.0}, :key-signature {}, :config {:type :midi, :patch 1}, :duration {:beats 1.0, :ms 0}, :min-duration nil, :volume 1.0, :last-offset #alda.lisp.model.records.AbsoluteOffset{:offset 750.0}, :id "piano-57rju", :quantization 0.9, :duration-inside-cram nil, :tempo 120, :panning 0.5, :current-marker :start, :time-scaling 1, :stock "midi-acoustic-grand-piano", :track-volume 0.7874015748031497}}, :markers {:start 0}, :cram-level 0, :global-attributes {}, :nicknames {}, :beats-tally-default nil}
```

#### Error handling

One consequence of parsing input asynchronously like this is that errors are not
thrown immediately. When an error occurs at an earlier stage in the parsing
pipeline, the error object is placed onto the channel so that a later stage can
handle it. Only during the score-building phase do we throw the error.

Notice what happens in the REPL when we try to parse a score that produces an
error:

```clojure
;; STAGE 1: tokenize
;; (The error is caught here and passed along through the pipeline.)
alda.parser=> (-> "piano: c8 d e f atoek;;ceo c/e/g" tokenize print-stream)
#object[clojure.core.async.impl.channels.ManyToManyChannel 0x31d8b6ba "clojure.core.async.impl.channels.ManyToManyChannel@31d8b6ba"]
[:name [1 1] "piano"]
[:colon [1 6] ":"]
[:note [1 8] "c"]
[:note-length [1 9] "8"]
[:note [1 11] "d"]
[:note [1 13] "e"]
[:note [1 15] "f"]
[:name [1 17] "atoek"]
#error {
 :cause "Unexpected ';' at line 1, column 22."
 :via
 [{:type java.lang.Exception
   :message "Unexpected ';' at line 1, column 22."
   :at [sun.reflect.NativeConstructorAccessorImpl newInstance0 "NativeConstructorAccessorImpl.java" -2]}]
 :trace
 [...]}
[:EOF [1 22]]

;; STAGE 2: parse events
alda.parser=> (-> "piano: c8 d e f atoek;;ceo c/e/g" tokenize parse-events print-stream)
#object[clojure.core.async.impl.channels.ManyToManyChannel 0x36df8301 "clojure.core.async.impl.channels.ManyToManyChannel@36df8301"]
{:event-type :part, :instrument-call {:names ["piano"]}, :events nil}
{:event-type :note, :letter :c, :accidentals [], :beats 0.5, :ms 0, :slur? nil}
{:event-type :note, :letter :d, :accidentals [], :beats nil, :ms nil, :slur? nil}
{:event-type :note, :letter :e, :accidentals [], :beats nil, :ms nil, :slur? nil}
{:event-type :note, :letter :f, :accidentals [], :beats nil, :ms nil, :slur? nil}
#error {
 :cause "Unexpected ';' at line 1, column 22."
 :via
 [{:type java.lang.Exception
   :message "Unexpected ';' at line 1, column 22."
   :at [sun.reflect.NativeConstructorAccessorImpl newInstance0 "NativeConstructorAccessorImpl.java" -2]}]
 :trace
 [...]}
:EOF

;; STAGE 3: aggregate events
alda.parser=> (-> "piano: c8 d e f atoek;;ceo c/e/g" tokenize parse-events aggregate-events print-stream)
#object[clojure.core.async.impl.channels.ManyToManyChannel 0x531ebf8a "clojure.core.async.impl.channels.ManyToManyChannel@531ebf8a"]
{:event-type :part, :instrument-call {:names ["piano"]}, :events nil}
{:event-type :note, :letter :c, :accidentals [], :beats 0.5, :ms 0, :slur? nil}
{:event-type :note, :letter :d, :accidentals [], :beats nil, :ms nil, :slur? nil}
{:event-type :note, :letter :e, :accidentals [], :beats nil, :ms nil, :slur? nil}
#error {
 :cause "Unexpected ';' at line 1, column 22."
 :via
 [{:type java.lang.Exception
   :message "Unexpected ';' at line 1, column 22."
   :at [sun.reflect.NativeConstructorAccessorImpl newInstance0 "NativeConstructorAccessorImpl.java" -2]}]
 :trace
 [...]}
{:event-type :note, :letter :f, :accidentals [], :beats nil, :ms nil, :slur? nil}

;; STAGE 4: build score
;; (At this point, the error is thrown.)
alda.parser=> (-> "piano: c8 d e f atoek;;ceo c/e/g" tokenize parse-events aggregate-events build-score print-stream)
#object[clojure.core.async.impl.channels.ManyToManyChannel 0x70bac373 "clojure.core.async.impl.channels.ManyToManyChannel@70bac373"]
Uncaught exception in thread async-dispatch-1:
                              java.lang.Thread.run              Thread.java:  745
java.util.concurrent.ThreadPoolExecutor$Worker.run  ThreadPoolExecutor.java:  617
 java.util.concurrent.ThreadPoolExecutor.runWorker  ThreadPoolExecutor.java: 1142
                                               ...
                 clojure.core.async/thread-call/fn                async.clj:  439
                           alda.parser/tokenize/fn               parser.clj:   43
              alda.parser.tokenize/read-character!             tokenize.clj:  668
                   alda.parser.tokenize/parse-name             tokenize.clj:  438
              alda.parser.tokenize/read-character!             tokenize.clj:  699
        alda.parser.tokenize/unexpected-char-error             tokenize.clj:   96
                  alda.parser.tokenize/emit-error!             tokenize.clj:   79
                                               ...
java.lang.Exception: Unexpected ';' at line 1, column 22.
    java.lang.Error: java.lang.Exception: Unexpected ';' at line 1, column 22.
```

