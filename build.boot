(set-env!
  :source-paths   #{"src" "test"}
  :resource-paths #{"examples"}
  :dependencies   '[
                    ; dev
                    [adzerk/bootlaces            "0.1.13"       :scope "test"]
                    [adzerk/boot-test            "1.1.2"        :scope "test"]
                    [alda/sound-engine-clj       "0.1.0"        :scope "test"]
                    [org.clojure/tools.namespace "0.3.0-alpha3" :scope "test"]
                    ; used in examples_test.clj
                    [io.aviso/pretty             "0.1.33"       :scope "test"]

                    ; alda.core
                    [org.clojure/clojure    "1.8.0"]
                    [org.clojure/core.async "0.2.395"]
                    [instaparse             "1.4.3"]
                    [com.taoensso/timbre    "4.7.4"]
                    [djy                    "0.1.4"]
                    [potemkin               "0.4.3"]
                    [clj_manifest           "0.2.0"]])

(require '[adzerk.bootlaces :refer :all]
         '[adzerk.boot-test :refer :all])

(def ^:const +version+ "0.1.2")

(bootlaces! +version+)

(task-options!
  pom     {:project 'alda/core
           :version +version+
           :description "The core machinery of Alda"
           :url "https://github.com/alda-lang/alda-core"
           :scm {:url "https://github.com/alda-lang/alda-core"}
           :license {"name" "Eclipse Public License"
                     "url" "http://www.eclipse.org/legal/epl-v10.html"}}

  jar     {:file "alda-core.jar"}

  install {:pom "alda/core"}

  target  {:dir #{"target"}}

  test    {:namespaces '#{; general tests
                          alda.parser.barlines-test
                          alda.parser.clj-exprs-test
                          alda.parser.event-sequences-test
                          alda.parser.comments-test
                          alda.parser.duration-test
                          alda.parser.events-test
                          alda.parser.octaves-test
                          alda.parser.repeats-test
                          alda.parser.score-test
                          alda.parser.variables-test
                          alda.lisp.attributes-test
                          alda.lisp.cram-test
                          alda.lisp.chords-test
                          alda.lisp.code-test
                          alda.lisp.duration-test
                          alda.lisp.global-attributes-test
                          alda.lisp.markers-test
                          alda.lisp.notes-test
                          alda.lisp.parts-test
                          alda.lisp.pitch-test
                          alda.lisp.score-test
                          alda.lisp.variables-test
                          alda.lisp.voices-test
                          alda.util-test

                          ; benchmarks / smoke tests
                          alda.examples-test}})

(deftask package
  "Builds jar file."
  []
  (comp (pom)
        (jar)))

(deftask deploy
  "Builds jar file, installs it to local Maven repo, and deploys it to Clojars."
  []
  (comp (package) (install) (push-release)))

