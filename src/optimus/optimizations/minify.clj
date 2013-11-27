(ns optimus.optimizations.minify
  (:require [optimus.v8 :as v8]))

(defn- escape [str]
  (-> str
      (clojure.string/replace "\\" "\\\\")
      (clojure.string/replace "'" "\\'")
      (clojure.string/replace "\n" "\\n")))

(defn- throw-v8-exception [#^String text path]
  (if (= (.indexOf text "ERROR: ") 0)
    (let [prefix (when path (str "Exception in " path ": "))
          error (clojure.core/subs text 7)]
      (throw (Exception. (str prefix error))))
    text))

(defn- js-minify-code [js options]
  (str "(function () {
    try {
        var ast = UglifyJS.parse('" (escape js) "');
        ast.figure_out_scope();
        var compressor = UglifyJS.Compressor();
        var compressed = ast.transform(compressor);
        compressed.figure_out_scope();
        compressed.compute_char_frequency();"
        (if (get options :mangle-js-names true) "compressed.mangle_names();" "")
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

(defn minify-js
  ([js] (minify-js js {}))
  ([js options] (minify-js (create-uglify-context) js options))
  ([context js options]
     (throw-v8-exception (v8/run-script-in-context context (js-minify-code js options))
                         (:path options))))

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
        var process;
        var compressor = new CSSOCompressor();
        var translator = new CSSOTranslator();
        var compressed = compressor.compress(srcToCSSP('" (escape css) "', 'stylesheet', true), " (not (get options :optimize-css-structure true)) ");
        return translator.translate(cleanInfo(compressed));
    } catch (e) { return 'ERROR: ' + e.message; }
}());"))

(def csso
  "The CSSO source code, free of dependencies and runnable in a
stripped context"
  (slurp (clojure.java.io/resource "csso.js")))

(defn create-csso-context []
  "Minify CSS with the bundled CSSO version"
  (let [context (v8/create-context)]
    (v8/run-script-in-context context csso)
    context))

(defn minify-css
  ([css] (minify-css css {}))
  ([css options] (minify-css (create-csso-context) css options))
  ([context css options]
     (throw-v8-exception (v8/run-script-in-context context (css-minify-code css options))
                         (:path options))))

(defn minify-css-asset
  [context asset options]
  (let [#^String path (:path asset)]
    (if (.endsWith path ".css")
      (update-in asset [:contents] #(minify-css context % (assoc options :path path)))
      asset)))

(defn minify-css-assets
  ([assets] (minify-css-assets assets {}))
  ([assets options]
     (let [context (create-csso-context)]
       (map #(minify-css-asset context % options) assets))))
