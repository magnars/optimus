(ns optimus.minify
  (:require [v8.core :as v8]))

(defn- escape [str]
  (-> str
      (clojure.string/replace "'" "\\'")
      (clojure.string/replace "\n" "\\n")))

(defn- throw-v8-exception [text]
  (if (= (.indexOf text "ERROR: ") 0)
    (throw (Exception. (clojure.core/subs text 7)))
    text))

(defn- js-minify-code [js {:keys [mangle-names] :or {mangle-names true}}]
  (str "(function () {
    try {
        var ast = UglifyJS.parse('" (escape js) "');
        ast.figure_out_scope();
        var compressor = UglifyJS.Compressor();
        var compressed = ast.transform(compressor);
        compressed.figure_out_scope();
        compressed.compute_char_frequency();"
        (if mangle-names "compressed.mangle_names();" "")
        "var stream = UglifyJS.OutputStream();
        compressed.print(stream);
        return stream.toString();
    } catch (e) { return 'ERROR: ' + e.message; }
}());"))

(defn minify-js-in-uglify-context [context js opt]
  "Given a V8 context with the UglifyJS global loaded, minify JS and return the
results as a string"
  (throw-v8-exception (v8/run-script-in-context context (js-minify-code js opt))))

(def uglify
  "The UglifyJS source code, free of dependencies and runnable in a
stripped context"
  (slurp (clojure.java.io/resource "uglify.js")))

(defn minify-js [js & opt]
  "Minify JS with the bundled UglifyJS version"
  (let [context (v8/create-context)]
    (v8/run-script-in-context context uglify)
    (minify-js-in-uglify-context context js (first opt))))

(defn- css-minify-code [css]
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
        var compressed = compressor.compress(srcToCSSP('" (escape css) "', \"stylesheet\", true));
        return translator.translate(cleanInfo(compressed));
    } catch (e) { return 'ERROR: ' + e.message; }
}());"))

(defn minify-css-in-csso-context [context css]
  "Given a V8 context with the CSSOCompressor, CSSOTranslator, cleanInfo and
srcToCSSP globals loaded, minify CSS and return the results as a string"
  (throw-v8-exception (v8/run-script-in-context context (css-minify-code css))))

(def csso
  "The CSSO source code, free of dependencies and runnable in a
stripped context"
  (slurp (clojure.java.io/resource "csso.js")))

(defn minify-css [css]
  "Minify CSS with the bundled CSSO version"
  (let [context (v8/create-context)]
    (v8/run-script-in-context context csso)
    (minify-css-in-csso-context context css)))
