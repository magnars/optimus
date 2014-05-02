(ns optimus.class-path
  (:require [clojure.string :as s]
            [clojure.java.io :as io])
  (:import [java.io File]
           [java.net URL URI]
           [java.util.zip ZipFile ZipEntry]))

;; there are major performance improvements to be gained by not
;; traversing the entirety of the class path when running locally and
;; picking up files from the class path for every request. Since
;; you're not serving files from a .m2 folder in production, this is
;; hopefully a safe bet.

(defn- ->url
  [^File f]
  (vector
    (.getCanonicalPath f)
    (-> f (.toURI) (.toURL))))

(defn- ->jar-url
  [^URL jar ^String path]
  (let [uri (URI. (str "jar:" (.toURI jar) "!" path))]
    (vector
      (or (.getPath uri) path)
      (.toURL uri))))

(defn- parse-jar-url
  [^URL url]
  (let [^String p (.getPath url)
        idx (.lastIndexOf p "!")]
    (vector
      (URL. (if (neg? idx) p (subs p 0 idx)))
      (when-not (neg? idx)
        (subs p (inc idx))))))

(defn- jar-contents
  [^File jar-file]
  (->> (ZipFile. jar-file)
       (.entries)
       (enumeration-seq)
       (map #(str "/" (.getName ^ZipEntry %)))
       (remove #(.endsWith ^String % "/"))
       (vec)))

(defn- list-jar-contents
  [^URL url]
  (let [[jar path] (parse-jar-url url)
        jar-file (io/file jar)]
    (when (.isFile jar-file)
      (let [contents (jar-contents jar-file)]
        (->> (if path
               (filter #(.startsWith ^String % path) contents)
               contents)
             (map #(->jar-url jar %)))))))

(defn- list-files
  [^File f]
  (when (.exists f)
    (letfn [(lazy-traverse [^File dir]
              (lazy-seq
                (let [fs (.listFiles dir)]
                  (mapcat
                    (fn [^File f]
                      (if (.isDirectory f)
                        (lazy-traverse f)
                        [(->url f)]))
                    fs))))]
      (if (.isDirectory f)
        (lazy-traverse f)
        [(->url f)]))))

(defn- resources
  [directory-path]
  (-> (Thread/currentThread)
      (.getContextClassLoader)
      (.getResources directory-path)
      (enumeration-seq)))

(defn resource-urls
  "Find all files residing in a directory of the given name within _all of the classpath_.
   Apply the given function to the path string and produce either the path string to use
   or nil.

   Returns a map of `[path-string -> URL]`."
  [directory-path path-fn]
  (let [^File f (io/file directory-path)
        files (list-files f)]
    (->> (for [^URL url (resources directory-path)]
           (case (.getProtocol url)
             "jar"  (list-jar-contents url)
             "file" (list-files (io/file url))
             nil))
         (apply concat files)
         (keep
           (fn [[path url]]
             (when-let [path' (path-fn path)]
               [path' url])))
         (into {}))))
