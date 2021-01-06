# alda-core

> **NOTE:** This was a component of Alda 1.x. It is no longer in use.

The core machinery of [Alda](https://github.com/alda-lang/alda), implemented in Clojure.

## Components

* **alda.lisp**: a Clojure DSL for building a musical score

* **alda.parser**: reads Alda code, interprets it as music events, and builds a score using  `alda.lisp`

For more details about how each component works, see the alda-core [development guide](doc/development-guide.md).

## Development

### Prerequisites

Development on the Alda core library requires that you have the [Boot](http://boot-clj.com) build tool installed. This allows you to run the tests and use other development tasks.

### `boot dev` task

For a convenient development experience, you can follow this workflow:

1. Make some changes to the code in this repo.

2. Run `boot dev`. This runs an Alda server in the foreground using the latest
   release version of
   [alda/server-clj](https://github.com/alda-lang/alda-server-clj) and your
   local copy of the code (with your changes) as the alda/core library.

   By default, the server will run on port 27714, but if you'd like, you can
   specify another port via the `-p` / `--port` option.

3. Now you can test your changes via the `alda` CLI, e.g.:

  ```shell
  $ alda -p 27714 play -c "piano: c"
  ```

  You might find it most convenient to start an Alda REPL connected to your
  development Alda server:

  ```shell
  $ alda -p 27714 repl
  ```

### `boot test` task

To run the unit test suite, run `boot test`.

When developing, you can run `boot watch test` in a separate terminal and the
tests will re-run every time you make a change to a file.

#### Adding tests

It is generally good to add new test cases when fixing bugs and adding features. [Test-driven development](https://en.wikipedia.org/wiki/Test-driven_development) is a good workflow when developing alda-core.

The automated test battery includes smoke tests where we parse and evaluate all of the example Alda scores in the `examples/` directory. If you add an additional example score, be sure to add it to the list of score files in `test/alda/examples_test.clj`.

### Logging

Alda uses [timbre](https://github.com/ptaoussanis/timbre) for logging. Every note event, attribute change, etc. is logged at the DEBUG level, which can be useful for debugging purposes.

The default logging level is WARN, so by default, you will not see these debug-level logs; you will only see warnings and errors.

To override this setting (e.g. for development and debugging), you can set the `TIMBRE_LEVEL` environment variable.

To see debug logs, for example, you can do this:

    export TIMBRE_LEVEL=:debug

When running tests via `boot test` and troubleshooting a failing test, it may be helpful to use debug-level logging by running `TIMBRE_LEVEL=:debug boot test`.

## License

Copyright © 2012-2019 Dave Yarwood et al

Distributed under the Eclipse Public License version 1.0.
