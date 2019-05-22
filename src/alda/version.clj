(ns alda.version
  (:require [manifest.core :refer (manifest)]))

(def ^:const -version-
  (or (:alda-version (manifest "alda.Main"))
      "unknown / development version"))
