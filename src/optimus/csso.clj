(ns optimus.csso
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [optimus.js :as js]))

(defn css-minification-code [css options]
  (str "csso.minify("
       "'" (js/escape (js/normalize-line-endings css)) "', "
       (json/write-str options) ").css"))

(def csso (slurp (io/resource "csso.js")))

(defn create-engine []
  (let [engine (js/make-engine)]
    (.eval engine csso)
    engine))

(defn minify
  ([css] (minify css {}))
  ([css options]
   (js/with-engine [engine (create-engine)]
     (minify engine css options)))
  ([engine css options]
   (js/run-script-with-error-handling
    engine
    (css-minification-code css options)
    (:path options))))

(comment

  (minify "body { color: red; }")

)
