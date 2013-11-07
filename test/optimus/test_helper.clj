(ns optimus.test-helper
  (:require [clojure.java.io :as io])
  (:import java.io.File))

(def tmp-dir "test/resources/with-files-tmp")

(def public-dir "with-files-tmp")

(defn app-that-returns-request [request]
  request)

(defmacro with-files [files & body]
  `(do
     (.mkdir (File. tmp-dir))
     (doseq [[path# contents#] ~files] (spit (str tmp-dir path#) contents#))
     (let [result# (do ~@body)]
       (doseq [[path# contents#] ~files] (io/delete-file (str tmp-dir path#)))
       (.delete (File. tmp-dir))
       result#)))

