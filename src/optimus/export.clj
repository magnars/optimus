(ns optimus.export
  (:require [clojure.java.io :as io])
  (:import [java.io FileOutputStream]))

(defn- create-folders [path]
  (.mkdirs (.getParentFile (io/file path))))

(defn- save-asset-to-path [asset path]
  (if-let [contents (:contents asset)]
    (spit path contents)
    (io/copy ((:get-stream asset))
             (FileOutputStream. (io/file path)))))

(defn save-assets [assets target-dir]
  (doseq [asset assets]
    (let [path (str target-dir (:path asset))]
      (create-folders path)
      (save-asset-to-path asset path))))

