(ns optimus.minify
  (:require [v8.core :as v8]))

(def uglify (slurp (.getFile (clojure.java.io/resource "uglify.js"))))

(defn minify-js-in-context [context js]
  (v8/run-script-in-context context (str uglify "
var ast = UglifyJS.parse('" js "');
ast.figure_out_scope();
var compressor = UglifyJS.Compressor();
var compressed = ast.transform(compressor);
compressed.figure_out_scope();
compressed.compute_char_frequency();
compressed.mangle_names();
var stream = UglifyJS.OutputStream();
compressed.print(stream);
stream.toString();")))

(def minify-js (partial minify-js-in-context (v8/create-context)))
