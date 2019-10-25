(ns optimus.assets.creation
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [optimus.paths :refer [just-the-filename filename-ext]]
            [optimus.class-path :refer [resource-urls]])
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
    (* (.intValue (/ modified 1000)) 1000)))

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

(defn create-binary-asset [public-dir path resource]
  (guard-path path)
  {:path path
   :resource resource
   :last-modified (last-modified resource)})

(defn load-text-asset [public-dir path resource]
  (create-asset
    path (slurp resource)
    :last-modified (last-modified resource)))

(defmulti load-asset
  (fn [_ ^String path ^URL resource]
    (filename-ext path)))

(defmethod load-asset :default
  [public-dir path resource]
  (create-binary-asset public-dir path resource))

(defn slice-path-to-after [public-dir #^String s]
  (subs s (+ (count public-dir)
             (.indexOf s (str public-dir "/")))))

(defn- emacs-file-artefact? [#^String path]
  (let [filename (just-the-filename path)]
    (or (.startsWith filename ".#")
        (and (.startsWith filename "#")
             (.endsWith filename "#")))))

(defn resource-urls-by-pattern
  [directory-path pattern]
  (let [pattern? (instance? java.util.regex.Pattern pattern)
        p? (comp
             (if pattern?
               #(when (and (not (emacs-file-artefact? %))
                           (re-find pattern %))
                  %)
               #{pattern})
             #(slice-path-to-after directory-path %))
        urls (resource-urls directory-path p?)]
    (when (empty? urls)
      (if pattern?
        (throw (Exception. (format "No files matched regex %s" pattern)))
        (throw (FileNotFoundException. pattern))))
    urls))

(defn- -load-assets
  [public-dir paths already-loaded-assets]
  (if (empty? paths)
    already-loaded-assets
    (let [urls (mapcat #(resource-urls-by-pattern public-dir %) paths)
          new-assets (mapv #(apply load-asset public-dir %) urls)
          references (mapcat :references new-assets)
          all-assets (concat already-loaded-assets new-assets)]
      (recur public-dir references all-assets))))

(defn load-assets
  [public-dir paths]
  (distinct (-load-assets public-dir paths nil)))

(defn load-bundle [public-dir bundle paths]
  (let [paths (mapcat #(keys (resource-urls-by-pattern public-dir %)) paths)
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
