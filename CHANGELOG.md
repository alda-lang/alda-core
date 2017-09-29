# CHANGELOG

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

[bbqbaron]: https://github.com/bbqbaron
[damiendevienne]: https://github.com/damiendevienne
[pzxwang]: https://github.com/pzxwang
