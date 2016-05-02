(ns optimus.optimizations.minify
  (:require [clojure.string :as str]
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

(defmacro with-context
  "Calls js/cleanup-engine on an already created context

usage:

(with-context [ctx (js/get-engine)]
   body)
"
  [[lname ctx] & body]
  `(let [~lname ~ctx]
     (try
       ~@body
       (finally
         (js/cleanup-engine ~lname)))))

(defn- throw-engine-exception [#^String text path]
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

(defn create-uglify-context
  ([]
     (create-uglify-context (js/get-engine)))
  ([engine]
     (.eval engine uglify)
     engine))

(defn- run-script-with-error-handling [context script file-path]
  (throw-engine-exception
   (try
     (.eval context script)
     (catch Exception e
       (str "ERROR: " (.getMessage e))))
   file-path))

(defn minify-js
  ([js] (minify-js js {}))
  ([js options]
   (with-context [context (create-uglify-context)]
     (minify-js context js options)))
  ([context js options]
   (if (looks-like-already-minified js)
     js
     (run-script-with-error-handling context (js-minify-code js (:uglify-js options)) (:path options)))))

(defn minify-js-asset
  [context asset options]
  (let [#^String path (:path asset)]
    (if (.endsWith path ".js")
      (update-in asset [:contents] #(minify-js context % (assoc options :path path)))
      asset)))

(defn minify-js-assets
  ([assets] (minify-js-assets assets {}))
  ([assets options]
   (with-context [context (create-uglify-context)]
     (doall (map #(minify-js-asset context % options) assets)))))

;; minify CSS

(defn prepare-css-source [css]
  (escape (normalize-line-endings css)))

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
  ([]
     (create-clean-css-context (js/get-engine)))
  ([engine]
     (.eval engine "var window = { XMLHttpRequest: {} };")
     (.eval engine clean-css)
     engine))

(defn minify-css
  ([css] (minify-css css {}))
  ([css options]
   (with-context [context (create-clean-css-context)]
     (minify-css context css options)))
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
   (with-context [context (create-clean-css-context)]
     (doall (map #(minify-css-asset context % options) assets)))))
