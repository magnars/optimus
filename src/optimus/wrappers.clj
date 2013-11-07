(ns optimus.wrappers
  (:require [clojure.java.io :as io]))

(defn ->file [public-dir file]
  {:path file
   :original-path file
   :contents (slurp (io/resource (str public-dir file)))})

(defn- ->bundled-files [bundle public-dir files]
  (map #(assoc (->file public-dir %) :bundle bundle) files))

(defn- concat-files [request files]
  (update-in request [:optimus-files] concat (doall files)))

(defn wrap-with-file-bundle [app bundle public-dir files]
  (fn [request]
    (app (concat-files request (->bundled-files bundle public-dir files)))))

(defn wrap-with-file-bundles [app public-dir bundles]
  (fn [request]
    (let [bundle-files (mapcat (fn [[bundle files]] (->bundled-files bundle public-dir files)) bundles)]
      (app (concat-files request bundle-files)))))

(defn wrap-with-files [app public-dir files]
  (fn [request]
    (app (concat-files request (map #(->file public-dir %) files)))))
