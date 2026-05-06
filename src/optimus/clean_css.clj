(ns optimus.clean-css
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [optimus.js :as js]))

(def default-clean-css-settings
  {:level 2})

(defn create-legacy-clean-css-settings [options]
  {:level {1 {:all true
              :specialComments (:keep-special-comments options "'all'")}
           2 {:all (:advanced-optimizations options true)}}
   :format (if (:keep-line-breaks options) "keep-breaks" false)
   :compatibility (:compatibility options "*")})

(defn is-legacy-clean-css-opts? [options]
  (seq (select-keys options [:keep-special-comments :advanced-optimizations :keep-line-breaks])))

(defn get-clean-css-settings [options]
  (-> (let [options (dissoc options :aggressive-merging)]
        (if (is-legacy-clean-css-opts? options)
          (create-legacy-clean-css-settings options)
          (or (not-empty options) default-clean-css-settings)))
      (assoc :inline false)))

(defn css-minification-code
  [css options]
  (str "(function () {
        var CleanCSS = require('clean-css');
        var source = '" (js/escape (js/normalize-line-endings css)) "';
        var options = " (json/write-str (get-clean-css-settings options)) ";
        var minified = new CleanCSS(options).minify(source).styles;
        return minified;
}());"))

(def clean-css
  "The clean-css source code, free of dependencies and runnable in a
  stripped context"
  (slurp (io/resource "clean-css.js")))

(defn create-engine
  "Minify CSS with the bundled clean-css version"
  []
  (let [engine (js/make-engine)]
    (.eval engine "var window = { XMLHttpRequest: {} };")
    (.eval engine clean-css)
    engine))

(defn minify-css
  ([css] (minify-css css {}))
  ([css options]
   (js/with-engine [engine (create-engine)]
     (minify-css engine css options)))
  ([engine css options]
   (js/run-script-with-error-handling
    engine
    (css-minification-code css options)
    (:path options))))
