(ns optimus.paths
  (:import (java.nio.file Paths Path)))

(defn create-path ^Path [str]
  (Paths/get str (make-array String 0)))

(defn- path-seq-to-url [path-seq]
  (str "/" (->> path-seq
                (map #(.toString %))
                (clojure.string/join "/"))))

(defn create-path-seq [^Path path]
  (-> path
      (.iterator)
      (iterator-seq)
      (vec)))

(defn ensure-trailing-slash [url-str]
  (if (clojure.string/ends-with? url-str "/")
    url-str
    (str url-str "/")))

(defn just-the-path [path]
  (-> (create-path path)
      (create-path-seq)
      (butlast)
      (path-seq-to-url)
      (ensure-trailing-slash)))

(defn just-the-filename [path]
  (-> (create-path path)
      (.getFileName)
      (.toString)))

(defn to-absolute-url [container-url relative-url]
  (let [[relative-url-path relative-url-query-string] (clojure.string/split relative-url #"\?" 0)
        res (-> (.resolveSibling (create-path container-url) (create-path relative-url-path))
                (.normalize)
                (create-path-seq)
                (path-seq-to-url))]
    (if relative-url-query-string
      (str res "?" relative-url-query-string)
      res)))

(defn filename-ext [filename]
  (second (re-find #"\.([^./\\]+)$" filename)))

