(ns optimus.optimizations.minify
  (:require
    [clojure.string :as str]
    [optimus.js :as js]))

(defn- escape [str]
  (-> str
      (str/replace "\\" "\\\\")
      (str/replace "'" "\\'")
      (str/replace "\n" "\\n")))

(defn looks-like-already-minified
  "Files with a single line over 5000 characters are considered already
  minified, and skipped. This avoid issues with huge bootstrap.css files
  and its ilk."
  [contents]
  (->> contents
       (str/split-lines)
       (some (fn [^String s] (> (.length s) 5000)))))

(defn normalize-line-endings [str]
  (-> str
      (str/replace "\r\n" "\n")
      (str/replace "\r" "\n")))

(defn- js-minification-code
  [js options]
  (str "(function () {
        var ast = UglifyJS.parse('" (escape (normalize-line-endings js)) "');
        ast.figure_out_scope();
        var compressor = UglifyJS.Compressor();
        var compressed = ast.transform(compressor);
        compressed.figure_out_scope();
        compressed.compute_char_frequency();"
        (if (get options :mangle-names true) "compressed.mangle_names();" "")
        "var stream = UglifyJS.OutputStream();
        compressed.print(stream);
        return stream.toString();
}());"))

(def ^String uglify
  "The UglifyJS source code, free of dependencies and runnable in a
  stripped context"
  (slurp (clojure.java.io/resource "uglify.js")))

(defn prepare-uglify-engine
  []
  (let [engine (js/make-engine)]
    (.eval engine uglify)
    engine))

(defn minify-js
  ([js] (minify-js js {}))
  ([js options]
   (js/with-engine [engine (prepare-uglify-engine)]
     (minify-js engine js options)))
  ([engine js options]
   (if (looks-like-already-minified js)
     js
     (js/run-script-with-error-handling
       engine
       (js-minification-code js (:uglify-js options))
       (:path options)))))

(defn minify-js-asset
  [engine asset options]
  (let [#^String path (:path asset)]
    (if (.endsWith path ".js")
      (update-in asset [:contents] #(minify-js engine % (assoc options :path path)))
      asset)))

(defn minify-js-assets
  ([assets] (minify-js-assets assets {}))
  ([assets options]
   (js/with-engine [engine (prepare-uglify-engine)]
     (doall (map #(minify-js-asset engine % options)
                 assets)))))

;; minify CSS

(defn- css-minification-code
  [css options]
  (str "(function () {
        var CleanCSS = require('clean-css');
        var source = '" (escape (normalize-line-endings css)) "';
        var options = {
            processImport: false,
            aggressiveMerging: " (:aggressive-merging options true) ",
            advanced: " (:advanced-optimizations options true) ",
            keepBreaks: " (:keep-line-breaks options false) ",
            keepSpecialComments: '" (:keep-special-comments options "*") "',
            compatibility: '" (:compatibility options "*") "'
        };
        var minified = new CleanCSS(options).minify(source).styles;
        return minified;
}());"))

(def clean-css
  "The clean-css source code, free of dependencies and runnable in a
  stripped context"
  (slurp (clojure.java.io/resource "clean-css.js")))

(defn prepare-clean-css-engine
  "Minify CSS with the bundled clean-css version"
  []
  (let [engine (js/make-engine)]
    (.eval engine "var window = { XMLHttpRequest: {} };")
    (.eval engine clean-css)
    engine))

(defn minify-css
  ([css] (minify-css css {}))
  ([css options]
   (js/with-engine [engine (prepare-clean-css-engine)]
     (minify-css engine css options)))
  ([engine css options]
   (if (looks-like-already-minified css)
     css
     (js/run-script-with-error-handling
       engine
       (css-minification-code css (:clean-css options))
       (:path options)))))

(defn minify-css-asset
  [engine asset options]
  (let [#^String path (:path asset)]
    (if (.endsWith path ".css")
      (update-in asset [:contents] #(minify-css engine % (assoc options :path path)))
      asset)))

(defn minify-css-assets
  ([assets] (minify-css-assets assets {}))
  ([assets options]
   (js/with-engine [engine (prepare-clean-css-engine)]
     (doall (map #(minify-css-asset engine % options) assets)))))
