(ns optimus.optimizations.minify
  (:require
    [clojure.string :as str]
    [clojure.java.data :as java.data]
    [optimus.js :as js]))

(defn- escape [str]
  (-> str
      (str/replace "\\" "\\\\")
      (str/replace "'" "\\'")
      (str/replace "\n" "\\n")))

(defmacro with-engine
  [[lname engine] & body]
  `(let [~lname ~engine]
     (try
       ~@body
       (finally
         (when (instance? java.lang.AutoCloseable ~lname)
           (try
             (.close ~lname)
             (finally nil)))))))

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
    } catch (e) { /*return 'ERROR: ' + e.message + ' (line ' + e.line + ', col ' + e.col + ')';*/ return {\"type\": \"js-error\", \"msg\": e.message, \"line\": e.line, \"col\": e.col}; }
}());"))

(def ^String uglify
  "The UglifyJS source code, free of dependencies and runnable in a
  stripped context"
  (slurp (clojure.java.io/resource "uglify.js")))

(defn prepare-uglify-engine
  []
  (let [engine (js/make-js-engine)]
    (.eval engine uglify)
    engine))

(defn- js-error?
  [obj]
  (and (map? obj)
       (= "js-error" (get obj "type"))))

(defn- throwing-js-exception
  [result path]
  (if (js-error? result)
    (let [line (int (get result "line"))
          col  (int (get result "col"))]
      (throw
        (ex-info (str
                   (if path (format "Exception in %s: " path) "")
                   (get result "msg")
                   (if (and line col) (format " (line %s, col %s)" line col) ""))
                 {:type ::js-error, :line line, :col col})))
    result))

(defn- run-script-with-error-handling
  [engine script file-path]
  (throwing-js-exception
   (try (java.data/from-java (.eval engine script))
        (catch Exception e
          {"type" "js-error", "msg" (.getMessage e)}))
   file-path))

(defn minify-js
  ([js] (minify-js js {}))
  ([js options]
   (with-engine [engine (prepare-uglify-engine)]
     (minify-js engine js options)))
  ([engine js options]
   (if (looks-like-already-minified js)
     js
     (run-script-with-error-handling
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
   (with-engine [engine (prepare-uglify-engine)]
     (doall (map #(minify-js-asset engine % options)
                 assets)))))

;; minify CSS

(defn- css-minification-code
  [css options]
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
    } catch (e) { /* return 'ERROR: ' + e.message;*/ return {\"msg\": e.message}; }
}());"))

(def clean-css
  "The clean-css source code, free of dependencies and runnable in a
  stripped context"
  (slurp (clojure.java.io/resource "clean-css.js")))

(defn prepare-clean-css-engine
  "Minify CSS with the bundled clean-css version"
  []
  (let [engine (js/make-js-engine)]
    (.eval engine "var window = { XMLHttpRequest: {} };")
    (.eval engine clean-css)
    engine))

(defn minify-css
  ([css] (minify-css css {}))
  ([css options]
   (with-engine [engine (prepare-clean-css-engine)]
     (minify-css engine css options)))
  ([engine css options]
   (if (looks-like-already-minified css)
     css
     (run-script-with-error-handling
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
   (with-engine [engine (prepare-clean-css-engine)]
     (doall (map #(minify-css-asset engine % options) assets)))))
