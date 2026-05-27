(ns optimus.uglify-js
  (:require [clojure.java.io :as io]
            [optimus.javascript :as js]))

(def scripts
  (future
    [(slurp (io/resource "babel.js"))
     (slurp (io/resource "uglify.js"))]))

(defn create-context []
  (js/create-context {:scripts @scripts}))

(defn transpile [context code]
  (-> (.getBindings context "js")
      (.getMember "Babel")
      (.getMember "transform")
      (.execute (into-array Object [code (js/clj->js context
                                           {:presets ["env"] :sourceType "script"})]))
      (.getMember "code")
      .asString))

(defn call-uglify-fn [context fn & args]
  (-> (.getBindings context "js")
      (.getMember "UglifyJS")
      (.getMember (name fn))
      (.execute (into-array Object args))))

(defn minify
  ([js]
   (minify js {}))
  ([js options]
   (with-open [context (create-context)]
     (minify context js options)))
  ([ctx js options]
   (let [ast (->> (cond->> js
                    (:transpile-es6? options) (transpile ctx))
                  (call-uglify-fn ctx :parse))]
     (js/call-method ast :figure_out_scope)
     (let [compressed (js/call-method ast :transform (call-uglify-fn ctx :Compressor))]
       (js/call-method compressed :figure_out_scope)
       (js/call-method compressed :compute_char_frequency)
       (when (get options :mangle-names true)
         (js/call-method compressed :mangle_names))
       (let [stream (call-uglify-fn ctx :OutputStream)]
         (js/call-method compressed :print stream)
         (.asString (js/call-method stream :toString)))))))
