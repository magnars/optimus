(ns optimus.export
  (:require [clojure.java.io :as io]
            [optimus.asset :as asset])
  (:import [java.io FileOutputStream]))

(defn create-folders [path]
  (.mkdirs (.getParentFile (io/file path))))

(defn save-asset-to-path [asset path]
  (if-let [contents (:contents asset)]
    (spit path contents)
    (with-open [input-stream (io/input-stream (:resource asset))
                output-stream (FileOutputStream. (io/file path))]
      (io/copy input-stream output-stream))))

(defn save-assets [assets target-dir]
  (doseq [asset assets]
    (let [path (str target-dir (asset/path asset))]
      (create-folders path)
      (save-asset-to-path asset path))))
