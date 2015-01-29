(ns optimus.optimizations.minify
  (:require [clojure.string :as str]
            [v8.core :as v8]))

(defn- escape [str]
  (-> str
      (str/replace "\\" "\\\\")
      (str/replace "'" "\\'")
      (str/replace "\n" "\\n")))

(defn- throw-v8-exception [#^String text path]
  (if (= (.indexOf text "ERROR: ") 0)
    (let [prefix (when path (str "Exception in " path ": "))
          error (clojure.core/subs text 7)]
      (throw (Exception. (str prefix error))))
    text))

(defn normalize-line-endings [str]
  (-> str
      (str/replace "\r\n" "\n")
      (str/replace "\r" "\n")))

(defn- js-minify-code [js options]
  (str "(function () {
    try {
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
    } catch (e) { return 'ERROR: ' + e.message + ' (line ' + e.line + ', col ' + e.col + ')'; }
}());"))

(def uglify
  "The UglifyJS source code, free of dependencies and runnable in a
  stripped context"
  (slurp (clojure.java.io/resource "uglify.js")))

(defn create-uglify-context []
  (let [context (v8/create-context)]
    (v8/run-script-in-context context uglify)
    context))

(defn- run-script-with-error-handling [context script file-path]
  (throw-v8-exception
   (try
     (v8/run-script-in-context context script)
     (catch Exception e
       (str "ERROR: " (.getMessage e))))
   file-path))

(defn minify-js
  ([js] (minify-js js {}))
  ([js options] (minify-js (create-uglify-context) js options))
  ([context js options]
   (run-script-with-error-handling context (js-minify-code js (:uglify-js options)) (:path options))))

(defn minify-js-asset
  [context asset options]
  (let [#^String path (:path asset)]
    (if (.endsWith path ".js")
      (update-in asset [:contents] #(minify-js context % (assoc options :path path)))
      asset)))

(defn minify-js-assets
  ([assets] (minify-js-assets assets {}))
  ([assets options]
   (let [context (create-uglify-context)]
     (map #(minify-js-asset context % options) assets))))

;; minify CSS

(defn- css-minify-code [css options]
  (str "
var console = {
    error: function (message) {
        throw new Error(message);
    }
};

(function () {
    try {
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
    } catch (e) { return 'ERROR: ' + e.message; }
}());"))

(def clean-css
  "The clean-css source code, free of dependencies and runnable in a
  stripped context"
  (slurp (clojure.java.io/resource "clean-css.js")))

(defn create-clean-css-context
  "Minify CSS with the bundled clean-css version"
  []
  (let [context (v8/create-context)]
    (v8/run-script-in-context context "var window = { XMLHttpRequest: {} };")
    (v8/run-script-in-context context clean-css)
    context))

(defn looks-like-already-minified
  "CSS files with a single line over 5000 characters is considered already
  minified, and skipped. This avoid issues with huge bootstrap.css files
  and its ilk."
  [css]
  (->> css
       (str/split-lines)
       (some (fn [^String s] (> (.length s) 5000)))))

(defn minify-css
  ([css] (minify-css css {}))
  ([css options] (minify-css (create-clean-css-context) css options))
  ([context css options]
   (if (looks-like-already-minified css)
     css
     (run-script-with-error-handling context (css-minify-code css (:clean-css options)) (:path options)))))

(defn minify-css-asset
  [context asset options]
  (let [#^String path (:path asset)]
    (if (.endsWith path ".css")
      (update-in asset [:contents] #(minify-css context % (assoc options :path path)))
      asset)))

(defn minify-css-assets
  ([assets] (minify-css-assets assets {}))
  ([assets options]
   (let [context (create-clean-css-context)]
     (map #(minify-css-asset context % options) assets))))
