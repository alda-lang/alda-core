# CHANGELOG

## 0.6.4 (2020-09-29)

* Fixed string formatting in the error message that you get when you place a
  marker at an unclear location. (See [#85][pr-85]). Thanks, [dhpiggott], for
  the contribution!

[pr-85]: https://github.com/alda-lang/alda-core/pull/85

## 0.6.3 (2020-06-15)

* Fixed a bug where in some cases, a global attribute can be recorded a fraction
  of a millisecond too late, potentially resulting in parts/voices not picking
  up on the attribute change until it's too late.

## 0.6.2 (2020-04-17)

* Fixed a minor bug related to alternate endings / repetitions, where input like
  the following would fail to parse correctly:

  ```
  [[c] [d]'1]*1
  ```

  The bug had to do with using event sequences in combination with the alternate
  endings feature.

## 0.6.1 (2020-03-14)

* Fixed a minor bug where the parser would fail to recognize that a note at the
  end of a part that ends with a `~` followed by a `|` is supposed to be
  slurred.

  In other words, it was treating `c4~ |` at the end of an instrument part as an
  un-slurred note, when it's supposed to be slurred.

* Fixed buggy error handling logic in the case of an unhandled exception.
  Before, we were inadvertently hiding the exception and the message ended up
  being "null." Now the exception message gets through.

## 0.6.0 (2019-08-16)

* New alda.lisp function, `midi-note`, is available as an alternative to
  `pitch` that is occasionally useful for algorithmic compositions, etc. For
  example, instead of `(note (pitch :c :sharp))`, you can specify the MIDI note
  number, `(note (midi-note 61))`.

## 0.5.4 (2019-05-05)

* Tabs can now be used as whitespace in an Alda score.

## 0.5.3 (2019-04-06)

* Instrument parts now track (local) tempo attribute changes. Instrument parts
  now have `:tempo/values`, a map of offset (ms) to tempo (bpm).

* Added the notion of `:tempo/role` to instrument parts. The first instrument
  added to a score has the `:tempo/role` `:master`.

* Added a top-level `:tempo/values` (also a map of offset (ms) to tempo (bpm))
  to the score. These values are a merger of `{0 120}` (a default initial tempo
  of 120 bpm), the `:tempo/values` of the part whose `:tempo/role` is `:master`,
  and any global tempo changes.

## 0.5.2 (2019-01-28)

* Made `alda.lisp.score/new-score` public.

## 0.5.1 (2019-01-19)

* Rewrote `alda.lisp.instruments.midi` (the definitions of all the MIDI
  instruments that Alda knows about) in a more data-oriented way.

  This will allow us to add an `alda instruments` command (and corresponding
  `:instruments` Alda REPL command) that can list out the instruments available.

## 0.5.0 (2018-12-01)

* New feature: alternate phrases during iterations of repeats.

  This will be documented soon in alda-lang/alda, but for now, see
  [#17](https://github.com/alda-lang/alda-core/issues/17) for more information.

## 0.4.0 (2018-06-22)

* Removed the "scheduled functions" feature, which isn't currently very useful
  and will be unsupported soon. See
  [#65](https://github.com/alda-lang/alda-core/pull/65) for more details.

## 0.3.10 (2018-02-28)

* Fixed a minor bug in the parser: there was an edge case where a "get variable"
  event wasn't being disambiguated from its earlier, less-specific "name" form
  if the "get variable" event happened to be the last thing in the definition of
  another variable. ([#64](https://github.com/alda-lang/alda-core/issues/64))

  Thanks to [elyisgreat] for spotting the bug!

## 0.3.9 (2018-02-16)

* Fixed a minor bug where parsing an invalid score like `piano:
  undefinedVariable` would return `nil` instead of throwing the error.

  The bug was that when a score is syntactically valid but throws an exception
  while trying to build the score (in the case of this example, because the
  referenced variable is undefined), the exception is thrown inside a core.async
  channel and does not affect the main thread -- essentially it gets swallowed,
  which is a known caveat of exceptions in core.async.

  Now, any errors thrown while building the score are passed through the parsing
  pipeline so that they can be thrown when we're ready to return a result (or
  throw an exception).

## 0.3.8 (2018-02-05)

* Fixed a bug where the parser did not correctly parse nested events in some
  situations, for example a set-variable expression containing a CRAM expression
  containing a chord. ([#55](https://github.com/alda-lang/alda-core/issues/55))

  Thanks to [elyisgreat] for reporting this issue!

## 0.3.7 (2017-10-30)

* Fixed a bug in the way the program path is determined when a server starts
  workers. (That code lives in alda.util, in this repo.) The bug was showing
  itself when the path to the `alda` (or `alda.exe`) executable contained spaces
  or other special characters.

  Thanks to [Hemaolle] for the detective work and PR to fix this issue!

## 0.3.6 (2017-10-17)

* Added a `reference-pitch` (alias: `tuning-constant`) attribute, which will
  have an affect on the pitch of each note in Hz. This number is the desired
  pitch of A4 (the note A in the 4th octave). The default value is 440 Hz.

  However, please note that this value is not currently used. We are still
  figuring out how to tune MIDI notes in Java -- it is more difficult that one
  might expect. If you're interested in helping with this, please let us know!

* Added a `transposition` (alias: `transpose`) attribute, which moves all notes
  (either per-instrument, or globally, depending on whether you are using
  `transpose` or `transpose!`) up or down by a desired number of semitones.
  Positive numbers represent increasing semitones, and negative numbers
  represent decreasing semitones.

  This attribute can be used to make writing parts for [transposing
  instruments](https://en.wikipedia.org/wiki/Transposing_instrument) more
  convenient. To see `transpose` in use, see [this example
  score](https://github.com/alda-lang/alda-core/blob/master/examples/jimenez-divertimento.alda),
  a transcription of a saxophone quartet by Juan Santiago Jiménez.

  Saxophones are transposing instruments; soprano and tenor saxophones are
  considered "Bb" instruments, and alto and baritone saxophones are considered
  "Eb" instruments.  This means that an instrument part written for a baritone
  saxophone, for example, might appear to be written in C major, but when read
  and performed by a baritone saxophonist, it will sound like Eb major, the
  intended key.

Thanks, [pzxwang], for implementing these new features!

## 0.3.5 (2017-10-14)

* Minor improvement to the new `tempo` function overload and `metric-modulation`
  function: the supplied note-length can be a string representing multiple note
  lengths tied together, e.g.:

  ```
  (tempo "4~16" 120)
  ```

  Thanks to [elyisgreat] for the issue and [pzxwang] for the pull request!

## 0.3.4 (2017-10-09)

* Added an overload of `tempo` that allows you to specify the tempo in terms of
  a note value other than (the default) a quarter note.

  For example, "♩. = 150" can be expressed as:

  ```
  (tempo! "4." 150)
  ```

  (NB: the note value can be either a number or a string containing a
  number followed by dots.)

  It is still OK to leave out the note value; the default behavior is to set the
  tempo relative to a quarter note. "♩ = 60" can still be expressed as:

  ```
  (tempo! 60)
  ```

* Added a new function, `metric-modulation`, which sets the tempo based on a
  [metric modulation](https://en.wikipedia.org/wiki/Metric_modulation), i.e.
  shifting from one meter to another.

  Say, for example, that you're writing a score that starts in 9/8 -- three
  beats per measure, where each beat is a dotted quarter note.

  At a certain point in the piece, you want to transition into a 3/2 section --
  still three beats per measure, but now each beat is a half note. You want the
  "pulse" to stay the same, but now each beat is subdivided into 4 half notes
  instead of 3. How do you do it?

  In traditional notation, it is common to see annotations like "♩. = 𝅗𝅥 " at the
  moment in the score where the time signature changes. This signifies that at
  that moment, the pulse stays the same, but the amount of time that used to
  represent a dotted quarter note now represents a half note. When the orchestra
  arrives at this point in the score, the conductor continues to conduct at the
  same "speed," but each musician mentally adjusts his/her perception of how to
  read his/her part, mentally subdividing each beat into 4 eighth notes instead
  of 3 eighth notes.

  In Alda, you can now express a metric modulation like "♩. = 𝅗𝅥 " as:

  ```
  (metric-modulation! "4." 2)
  ```

Thanks, [pzxwang], for the PR to add these new features!

## 0.3.3 (2017-10-02)

* Added the following modes:

  * `:ionian`
  * `:dorian`
  * `:phrygian`
  * `:lydian`
  * `:mixolydian`
  * `:aeolian`
  * `:locrian`

  These can be used as an alternative to `:major` and `:minor` when specifying a
  key signature.

  For example:

  ```
  piano:
    (key-sig [:d :locrian])
    d8 e f g a b > c d8~1
  ```

  Thanks, [iggar], for this contribution!

## 0.3.2 (2017-09-29)

* Fixed a parser bug where a rest `r` followed by a newline inside of a variable
  definition would not be considered part of the variable definition.

  Thanks, [elyisgreat], for reporting this issue!

## 0.3.1 (2017-09-28)

Thanks, [pzxwang] for contributing the changes in this release in PR [#50](https://github.com/alda-lang/alda-core/pull/50)!

* Non-integer decimal note lengths are now accepted. For example, `c0.5` (or a
  double whole note, in Western classical notation) is twice the length of `c1`
  (a whole note).

* Added a convenient `set-note-length` function to alda.lisp.

  This is an alternative to `set-duration`, which, somewhat unintuitively, sets
  the current duration to its argument as a number of beats.

  To set the note length to a quarter note (1 beat), for example, you can now
  use either `(set-duration 1)` or `(set-note-length 4)`.

## 0.3.0 (2017-06-17)

* Fixed error handling in `parse-input` when parsing in `:score` mode (which is
  the default). I overlooked the fact that core-async `go-loop` doesn't play
  nice with error handling, so you have to do something like send the error on
  the channel and then throw it outside of the `go-loop`.

  Before this fix, if an error occurred, the server would attempt to send the
  exception object itself as a success response and then fail because the
  exception is not serializable as JSON.

  Now, with `parse-input` properly throwing exceptions, the server will send an
  error response if one is thrown.

## 0.2.2 (2017-05-31)

* Fixed issue [#41](https://github.com/alda-lang/alda-core/issues/41), where `r`
  followed by e.g. `]` would trigger a parser error.

## 0.2.1 (2017-05-28)

* Fixed a handful of bugs in the new parser implementation where a one-line
  variable definition, e.g.:

  ```
  foo = d8 e f+ g a b4.
  ```

  ...might fail to parse if it ends with certain events.

## 0.2.0 (2017-05-27)

* Re-implemented the parser from the ground up in a more efficient way. The new
  parser implementation uses core.async channels to complete the stages of the
  parsing pipeline in parallel.

  Performance is roughly the same (only slightly better) for scores under ~100
  lines, but significantly better for larger scores.

  More importantly, parsing asynchronously opens the door for us to make playing
  a score happen almost immediately in the near future.

  See [#37](https://github.com/alda-lang/alda-core/pull/37) for more details.

* An added benefit of the new parser implementation is that it fixes issue
  [#12](https://github.com/alda-lang/alda-core/issues/12). Line and column
  numbers are now correct, and error messages are more informative when a score
  fails to parse.

* The `alda.parser-util` namespace, which included the `parse-to-*-with-context`
  functions, has been removed. See [this commit](https://github.com/alda-lang/alda-core/pull/37/commits/5f35d659927952e99ea7ec9ab0ee2f4bb2f681aa) for more details.

* The Alda parser no longer generates alda.lisp code.

  Originally, the Alda parser created a score by generating alda.lisp code and
  then evaluating it.  This actually changed some time ago to a system where the
  parser generated a sequence of events directly and then used them to build the
  score. We kept the code that generates alda.lisp code, even though it was no
  longer an implementation detail of the parser, just an alternate "mode" of
  parsing.

  With these changes to the parser, it would take some additional work to
  generate alda.lisp code. Since it is no longer necessary to do that,
  generating alda.lisp code is no longer a feature of Alda. We could
  re-implement this feature in the future as part of the new parser, if there is
  a demand for it.

* Miscellaneous implementation changes that could be relevant if you use Alda
  as a Clojure library:

  * `alda.parser/parse-input` returns a score map, rather than an unevaluated
    S-expression. Calling this function will require and refer `alda.lisp` for
    you if you haven't already done so in the namespace where you're using it.

  * `alda.lisp/alda-code` does not throw an exception by itself if the code is
    not valid Alda; instead, the output contains an Exception object, which gets
    thrown when used inside of a score

  * Whereas `alda.lisp/pitch` used to return a function to be applied to the
    current octave and key signature, now it returns a map that includes its
    `:letter` and `:accidentals`. This is more consistent with other alda.lisp
    functions, and it allows notes to have equality semantics.

    In other words, whereas `(= (note (pitch :c)) (note (pitch :c)))` used to be
    `false`, now it is `true` because we aren't comparing anonymous functions.

  * `(alda.lisp/barline)` now returns `{:event-type :barline}` instead of `nil`.

## 0.1.2 (2016-12-05)

* Fixed [#27](https://github.com/alda-lang/alda-core/issues/27), a bug where, when using note durations specified in seconds/milliseconds, the subsequent "default" note duration was not being set.

  Thanks to [damiendevienne] for reporting this bug!

## 0.1.1 (2016-11-20)

* Removed the `voices` (voice group) event, as [bbqbaron] and I figured out that it's not necessary. It turns out that each `voice` event manages its voice group implicitly. For more discussion, see [alda-lang/alda#286](https://github.com/alda-lang/alda/pull/286).

## 0.1.0 (2016-11-19)

* Extracted alda-core from the [main Alda repo](https://github.com/alda-lang/alda) as of version 1.0.0-rc50.

[elyisgreat]: https://github.com/elyisgreat
[bbqbaron]: https://github.com/bbqbaron
[damiendevienne]: https://github.com/damiendevienne
[pzxwang]: https://github.com/pzxwang
[iggar]: https://github.com/iggar
[Hemaolle]: https://github.com/Hemaolle
[dhpiggott]: https://github.com/dhpiggott
