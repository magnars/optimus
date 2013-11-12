(ns optimus.assets
  (:require [clojure.java.io :as io])
  (:import java.io.FileNotFoundException))

(defn asset [path contents & {:as opts}]
  (when-not (.startsWith path "/")
    (throw (Exception. (str "Asset paths must start with a slash. Got: " path))))
  (merge {:path path
          :contents contents}
         opts))

(defn- existing-resource [public-dir path]
  (or (io/resource (str public-dir path))
      (throw (FileNotFoundException. path))))

(defn load-one [public-dir path]
  (asset path (slurp (existing-resource public-dir path))))

(defn load-all [public-dir paths]
  (map #(load-one public-dir %) paths))

(defn original-path [asset]
  (or (:original-path asset)
      (:path asset)))
