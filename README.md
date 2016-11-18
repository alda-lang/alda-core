# alda-core

The core machinery of Alda, implemented in Clojure.

## Components

* **alda.parser** (reads Alda code and transforms it into Clojure code in the context of the `alda.lisp` namespace)

* **alda.lisp** (a Clojure DSL which provides the context for evaluating an Alda score, in its Clojure code form)

* **alda.sound** (generates sound based on the data map produced by parsing and evaluating Alda code)

* **alda.now** (an entrypoint for using Alda as a Clojure library)

* **alda.repl** (an interactive **R**ead-**E**val-**P**lay **L**oop for Alda code)

For more details about how each component works, see the alda-core [development guide](doc/development-guide.md).

## Development

### Prerequisites

Development on the Alda core library requires that you have the [Boot](http://boot-clj.com) build tool installed. This allows you to run the `test` and `dev` tasks to run tests and run the Alda REPL locally for development.

### `boot test` task

To run the unit test suite, run `boot test`.

#### Adding tests

It is generally good to add to the existing tests wherever it makes sense, i.e. whenever there is a new test case that Alda needs to consider. [Test-driven development](https://en.wikipedia.org/wiki/Test-driven_development) is a good idea.

If you find yourself adding a new file to the tests, be sure to add its namespace to the list of test namespaces in `build.boot` so that it will be included when you run the tests.

The automated test battery includes smoke tests where we parse and evaluate all of the example Alda scores in the `examples/` directory. If you add an additional example score, be sure to add it to the list of score files in `test/alda/examples_test.clj`.

### `boot dev` task

You can use the `boot dev` task to run an Alda REPL with any local changes. You may find this convenient for experimenting in Alda after making changes to the code.

> NOTE: We eventually want to [rewrite the Alda REPL as part of the Java client](https://github.com/alda-lang/alda/issues/154).

## License

Copyright © 2016 Dave Yarwood et al

Distributed under the Eclipse Public License version 1.0.
