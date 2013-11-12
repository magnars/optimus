(ns optimus.minify
  (:require [v8.core :as v8]))

(defn minify-js-in-uglify-context [context js]
  "Given a V8 context with the UglifyJS global loaded, minify JS and return the
results as a string"
  (v8/run-script-in-context context (str "
var ast = UglifyJS.parse('" (clojure.string/replace js "'" "\\'") "');
ast.figure_out_scope();
var compressor = UglifyJS.Compressor();
var compressed = ast.transform(compressor);
compressed.figure_out_scope();
compressed.compute_char_frequency();
compressed.mangle_names();
var stream = UglifyJS.OutputStream();
compressed.print(stream);
stream.toString();")))

(def uglify
  "The UglifyJS source code, free of dependencies and runnable in a
stripped context"
  (slurp (clojure.java.io/resource "uglify.js")))

(defn minify-js [js]
  "Minify JS with the bundled UglifyJS version"
  (let [context (v8/create-context)]
    (v8/run-script-in-context context uglify)
    (minify-js-in-uglify-context context js)))
