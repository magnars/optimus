(ns optimus.csso
  (:require [clojure.java.io :as io]
            [optimus.javascript :as js])
  (:import [org.graalvm.polyglot Context]))

(def scripts
  (future [(slurp (io/resource "csso.js"))]))

(defn create-context []
  (js/create-context {:scripts @scripts}))

(defn minify
  ([css]
   (minify css {}))
  ([css options]
   (with-open [context (create-context)]
     (minify context css options)))
  ([^Context context css options]
   (let [csso (-> (.getBindings context "js")
                  (.getMember "csso"))]
     (-> (js/call-method csso :minify css (js/clj->js context options))
         (js/get-string :css)))))
