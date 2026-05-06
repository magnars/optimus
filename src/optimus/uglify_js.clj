(ns optimus.uglify-js
  (:require [clojure.java.io :as io]
            [optimus.js :as js]))

(def ^String babel
  (slurp (io/resource "babel.js")))

(defn js-minification-code
  [js options]
  (let [js-code (js/escape (js/normalize-line-endings js))]
    (str "(function () {"
         (if (:transpile-es6? options)
           (str "var jsCode = Babel.transform('" js-code "', { presets: ['env'], sourceType: 'script' }).code;")
           (str "var jsCode = '" js-code "';"))
         "var ast = UglifyJS.parse(jsCode);
          ast.figure_out_scope();
          var compressor = UglifyJS.Compressor();
          var compressed = ast.transform(compressor);
          compressed.figure_out_scope();
          compressed.compute_char_frequency();"
         (if (get options :mangle-names true) "compressed.mangle_names();" "")
         "var stream = UglifyJS.OutputStream();
          compressed.print(stream);
          return stream.toString();
}());")))

(def ^String uglify
  "The UglifyJS source code, free of dependencies and runnable in a
  stripped context"
  (slurp (io/resource "uglify.js")))

(defn create-engine
  []
  (let [engine (js/make-engine)]
    (.eval engine uglify)
    (.eval engine babel)
    engine))

(defn minify
  ([js] (minify js {}))
  ([js options]
   (js/with-engine [engine (create-engine)]
     (minify engine js options)))
  ([engine js options]
   (js/run-script-with-error-handling
    engine
    (js-minification-code js options)
    (:path options))))
