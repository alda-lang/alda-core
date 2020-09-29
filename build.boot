(set-env!
  :source-paths   #{"src" "test"}
  :resource-paths #{"examples"}
  :dependencies   '[
                    ; dev
                    [adzerk/bootlaces            "0.2.0"  :scope "test"
                     ;; dev dependencies that accidentally got included in the
                     ;; deployed jar. whoops.
                     :exclusions [boot/new
                                  cpmcdaniel/boot-with-pom]]
                    [adzerk/boot-test            "1.2.0"  :scope "test"]
                    [org.clojure/tools.namespace "1.0.0"  :scope "test"]
                    [alda/server-clj             "LATEST" :scope "test"]
                    [alda/sound-engine-clj       "LATEST" :scope "test"]
                    ; used in examples_test.clj
                    [io.aviso/pretty             "0.1.37" :scope "test"]

                    ; alda.core
                    [org.clojure/clojure    "1.10.1"]
                    [org.clojure/core.async "1.0.567"]
                    [com.taoensso/timbre    "4.10.0"]
                    [djy                    "0.2.1"]
                    [potemkin               "0.4.5"]
                    [clj_manifest           "0.2.0"]])

(require '[adzerk.bootlaces :refer :all]
         '[adzerk.boot-test :refer :all])

(def ^:const +version+ "0.6.4")

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

  test    {:include #"-test$"})

;; This task is a work in progress.
;;
;; Each time the -main function is called, it starts a server and workers, and
;; they just run forever until you Ctrl-C, killing the entire `boot dev`
;; process.
;;
;; I'd like to make it so that each time there is a change to the fileset, the
;; server and workers stop gracefully, the namespace is reloaded (:reload below
;; doesn't seem to do the trick... I'm probably missing something), and the
;; server and workers are started again with the latest code.
(deftask dev
  "Runs an Alda server using the latest alda/server-clj and the local copy of
   the code in this repo for alda/core."
  [p port    PORT    int "The port on which to run the server. (default: 27714)"
   w workers WORKERS int "The number of workers to start. (default: 2)"]
  (comp
    (watch)
    (with-pass-thru _
      (require 'alda.dev :reload)
      ((resolve 'alda.dev/-main) (or port 27714) (or workers 2)))))

(deftask package
  "Builds jar file."
  []
  (comp (pom)
        (jar)))

(deftask deploy
  "Builds jar file, installs it to local Maven repo, and deploys it to Clojars."
  []
  (comp (package) (install) (push-release)))

