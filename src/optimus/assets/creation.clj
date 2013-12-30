(ns optimus.assets.creation
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [pathetic.core :as pathetic]
            [optimus.class-path :refer [file-paths-on-class-path]])
  (:import [java.io FileNotFoundException]
           [java.net URL]
           [java.util.zip ZipFile ZipEntry]))

(defn guard-path [#^String path]
  (when-not (.startsWith path "/")
    (throw (Exception. (str "Asset paths must start with a slash. Got: " path)))))

(defn create-asset [path contents & {:as opts}]
  (guard-path path)
  (merge {:path path
          :contents contents}
         opts))

(defn original-path [asset]
  (or (:original-path asset)
      (:path asset)))

(defn existing-resource [public-dir path]
  (or (io/resource (str public-dir path))
      (throw (FileNotFoundException. path))))

(defn- file-last-modified [#^URL resource]
  (let [url-connection (.openConnection resource)
        modified (.getLastModified url-connection)]
    (.close (.getInputStream url-connection))
    modified))

(defn- jar-file-last-modified [#^URL resource]
  (let [[jar-path file-path] (-> (.getPath resource)
                                 (subs 5)
                                 (str/split #"!/"))]
    (->> jar-path
         (ZipFile.)
         (.entries)
         (enumeration-seq)
         (filter (fn [#^ZipEntry e] (= file-path (.getName e))))
         (first)
         (.getTime))))

(defn last-modified [#^URL resource]
  (case (.getProtocol resource)
    "jar" (jar-file-last-modified resource)
    "file" (file-last-modified resource)
    nil))

(defn create-binary-asset [public-dir path]
  (guard-path path)
  (let [resource (existing-resource public-dir path)]
    {:path path
     :get-stream #(io/input-stream resource)
     :last-modified (last-modified resource)}))

(defn- load-text-asset [public-dir path]
  (let [resource (existing-resource public-dir path)]
    (create-asset path (slurp resource)
                  :last-modified (last-modified resource))))

(defn- filename-ext
  "Returns the file extension of a filename or filepath."
  [filename]
  (second (re-find #"\.([^./\\]+)$" filename)))

(defmulti load-asset (fn [_ path] (filename-ext path)))

(defmethod load-asset :default
  [public-dir path]
  (create-binary-asset public-dir path))

(defmethod load-asset "js" [public-dir path] (load-text-asset public-dir path))
(defmethod load-asset "html" [public-dir path] (load-text-asset public-dir path))
;; css is covered by load-css

(defn- load-asset-and-refs [public-dir path]
  (let [asset (load-asset public-dir path)]
    (concat [asset] (mapcat #(load-asset-and-refs public-dir %) (:references asset)))))

(defn slice-path-to-after [public-dir #^String s]
  (subs s (+ (count public-dir)
             (.indexOf s (str public-dir "/")))))

(defn- just-the-filename [path]
  (last (pathetic/split path)))

(defn- emacs-file-artefact? [#^String path]
  (.startsWith (just-the-filename path) ".#"))

(defn realize-regex-paths [public-dir path]
  (if (instance? java.util.regex.Pattern path)
    (let [paths (->> (file-paths-on-class-path)
                     (filter (fn [#^String p] (.contains p public-dir)))
                     (remove emacs-file-artefact?)
                     (map #(slice-path-to-after public-dir %))
                     (filter #(re-find path %)))]
      (if (empty? paths)
        (throw (Exception. (str "No files matched regex " path)))
        paths))
    [path]))

(defn load-assets [public-dir paths]
  (->> paths
       (mapcat #(realize-regex-paths public-dir %))
       (mapcat #(load-asset-and-refs public-dir %))
       (distinct)))

(defn load-bundle [public-dir bundle paths]
  (let [paths (mapcat #(realize-regex-paths public-dir %) paths)
        assets (load-assets public-dir paths)
        path-set (set paths)
        set-bundle-for-original-files (fn [asset] (if (contains? path-set (original-path asset))
                                                    (assoc asset :bundle bundle)
                                                    asset))]
    (map set-bundle-for-original-files assets)))

(defn load-bundles [public-dir bundles]
  (mapcat (fn [[bundle paths]]
            (load-bundle public-dir bundle paths))
          bundles))
