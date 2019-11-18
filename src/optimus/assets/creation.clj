(ns optimus.assets.creation
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [optimus.paths :refer [just-the-filename filename-ext]]
            [optimus.class-path :refer [file-paths-on-class-path]])
  (:import [java.io FileNotFoundException]
           [java.nio.file FileSystems Paths]
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

(defn create-binary-asset [public-dir path]
  (guard-path path)
  (let [resource (existing-resource public-dir path)]
    {:path path
     :resource resource
     :last-modified (last-modified resource)}))

(defn load-text-asset [public-dir path]
  (let [resource (existing-resource public-dir path)]
    (create-asset path (slurp resource)
                  :last-modified (last-modified resource))))

(defmulti load-asset (fn [_ path] (filename-ext path)))

(defmethod load-asset :default
  [public-dir path]
  (create-binary-asset public-dir path))

(defn- load-asset-and-refs [public-dir path]
  (let [asset (load-asset public-dir path)]
    (concat [asset] (mapcat #(load-asset-and-refs public-dir %) (:references asset)))))

(defn slice-path-to-after [public-dir #^String s]
  (subs s (+ (count public-dir)
             (.indexOf s (str public-dir "/")))))

(defn- emacs-file-artefact? [#^String path]
  (let [filename (just-the-filename path)]
    (or (.startsWith filename ".#")
        (and (.startsWith filename "#")
             (.endsWith filename "#")))))

(defn realize-paths-with-matcher [public-dir matcher-type matcher-val path-matcher]
  (let [paths (->> (file-paths-on-class-path)
                   (filter (fn [#^String p] (.contains p public-dir)))
                   (remove emacs-file-artefact?)
                   (map #(slice-path-to-after public-dir %))
                   (filter #(path-matcher %)))]
    (if (empty? paths)
      (throw (Exception. (str "No files matched " (name matcher-type) " " matcher-val)))
      paths)))

(defmulti realize-path (fn [public-dir [type val]] type))

(defmethod realize-path :path [public-dir [type val]]
  [val])

(defmethod realize-path :regex [public-dir [type val]]
  (realize-paths-with-matcher public-dir type val #(re-find val %)))

(defmethod realize-path :glob [public-dir [type val]]
  (let [glob-expression (str "glob:" val)
        path-matcher (.getPathMatcher (FileSystems/getDefault) glob-expression)]
    (realize-paths-with-matcher public-dir type val #(.matches path-matcher (Paths/get % (make-array String 0))))))

(defn normalize-path [path]
  (cond
    (string? path) [:path path]
    (instance? java.util.regex.Pattern path) [:regex path]
    (vector? path) path
    :else (IllegalArgumentException. (str "Provided path " path " must be a string, a regex or a vector"))))

(defn load-assets [public-dir paths]
  (->> paths
       (mapcat #(realize-path public-dir (normalize-path %)))
       (mapcat #(load-asset-and-refs public-dir %))
       (distinct)))

(defn load-bundle [public-dir bundle paths]
  (let [paths (mapcat #(realize-path public-dir (normalize-path %)) paths)
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
