(ns optimus.prime
  (:require [clojure.java.io :as io]))

(defn ->bundled-file [bundle public-dir file]
  {:path file
   :original-path file
   :contents (slurp (io/resource (str public-dir file)))
   :bundle bundle})

(defn wrap-with-file-bundle [app bundle public-dir files]
  (fn [request]
    (app (assoc request :optimus-files
                (doall (map #(->bundled-file bundle public-dir %) files))))))
