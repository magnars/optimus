(ns optimus.class-path
  (:require [clojure.string :as s])
  (:import [java.io File]
           [java.util.zip ZipFile ZipEntry]))

(defn class-path-elements []
  (->> (s/split (System/getProperty "java.class.path" ".") #":")
       (remove (fn [#^String s] (.contains s "/.m2/")))))
;; there are major performance improvements to be gained by not
;; traversing the entirety of the class path when running locally and
;; picking up files from the class path for every request. Since
;; you're not serving files from a .m2 folder in production, this is
;; hopefully a safe bet.

(defn get-file-paths [#^File file]
  (if (.isDirectory file)
    (mapcat get-file-paths (.listFiles file))
    [(.getCanonicalPath file)]))

(defn get-jar-paths [jar]
  (if-let [zip (try
                 (java.util.zip.ZipFile. jar)
                 (catch Exception e nil))]
    (->> zip
         (.entries)
         (enumeration-seq)
         (map (fn [#^ZipEntry e] (.getName e))))
    []))

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
