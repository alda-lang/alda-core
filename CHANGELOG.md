# CHANGELOG

## 0.1.2 (2016-12-05)

* Fixed [#27](https://github.com/alda-lang/alda-core/issues/27), a bug where, when using note durations specified in seconds/milliseconds, the subsequent "default" note duration was not being set.

  Thanks to [damiendevienne] for reporting this bug!

## 0.1.1 (2016-11-20)

* Removed the `voices` (voice group) event, as [bbqbaron] and I figured out that it's not necessary. It turns out that each `voice` event manages its voice group implicitly. For more discussion, see [alda-lang/alda#286](https://github.com/alda-lang/alda/pull/286).

## 0.1.0 (2016-11-19)

* Extracted alda-core from the [main Alda repo](https://github.com/alda-lang/alda) as of version 1.0.0-rc50.

[bbqbaron]: https://github.com/bbqbaron
[damiendevienne]: https://github.com/damiendevienne
