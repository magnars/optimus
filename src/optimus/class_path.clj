(ns optimus.class-path
  (:require [clojure.string :as s])
  (:import [java.io File]
           [java.util.zip ZipFile]))

(defn class-path-elements []
  (s/split (System/getProperty "java.class.path" ".") #":"))

(defn get-file-paths [file]
  (if (.isDirectory file)
    (mapcat get-file-paths (.listFiles file))
    [(.getCanonicalPath file)]))

(defn get-jar-paths [jar]
  (->> jar
       (java.util.zip.ZipFile.)
       (.entries)
       (enumeration-seq)
       (map #(.getName %))))

(defn get-resource-paths [path]
  (let [path-plus-slash-length (inc (count path))
        chop-path #(subs % path-plus-slash-length)
        file (File. path)]
    (cond
     (.isDirectory file) (get-file-paths file)
     (.exists file) (get-jar-paths file)
     :else [])))

(defn file-paths-on-class-path []
  (mapcat get-resource-paths (class-path-elements)))
