(ns optimus.assets
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [pathetic.core :as pathetic]
            [optimus.class-path :refer [file-paths-on-class-path]])
  (:import java.io.FileNotFoundException))

;; create-asset

(defn guard-path [#^String path]
  (when-not (.startsWith path "/")
    (throw (Exception. (str "Asset paths must start with a slash. Got: " path)))))

(defn create-asset [path contents & {:as opts}]
  (guard-path path)
  (merge {:path path
          :contents contents}
         opts))

(defn- existing-resource [public-dir path]
  (or (io/resource (str public-dir path))
      (throw (FileNotFoundException. path))))

(defn create-binary-asset [public-dir path & {:as opts}]
  (guard-path path)
  (let [resource (existing-resource public-dir path)]
    (merge {:path path
            :get-stream #(io/input-stream resource)})))

(defn original-path [asset]
  (or (:original-path asset)
      (:path asset)))

;; load-css-asset

(defn- just-the-path [url]
  (-> url
      pathetic/parse-path
      pathetic/up-dir
      pathetic/render-path
      pathetic/ensure-trailing-separator))

(defn- remove-url-appendages [s]
  (first (str/split s #"[\?#]")))

(defn combine-paths [container-url relative-url]
  (pathetic/normalize (pathetic/resolve (just-the-path container-url)
                                        (remove-url-appendages relative-url))))

(def css-url-re #"url\(['\"]?([^\)]+?)['\"]?\)")

(defn- css-url-str [url]
  (str "url('" url "')"))

(defn- is-data-url [#^String url]
  (.startsWith url "data:"))

(defn- replace-css-urls [file replacement-fn]
  (assoc-in file [:contents]
            (str/replace (:contents file) css-url-re
                         (fn [[match url]] (if (is-data-url url)
                                             match
                                             (css-url-str (replacement-fn file url)))))))

(defn paths-in-css [file]
  (->> file :contents
       (re-seq css-url-re)
       (map second)
       (remove is-data-url)
       (map #(combine-paths (original-path file) %))))

(defn- load-css-asset [public-dir path]
  (let [contents (slurp (existing-resource public-dir path))
        asset (-> (create-asset path contents)
                  (replace-css-urls #(combine-paths (original-path %1) %2)))]
    (assoc asset :references (set (paths-in-css asset)))))

;; load-text-asset

(defn- load-text-asset [public-dir path]
  (create-asset path (slurp (existing-resource public-dir path))))

;; load-assets

(defn- load-asset [public-dir #^String path]
  (guard-path path)
  (cond
   (.endsWith path ".css") (load-css-asset public-dir path)
   (.endsWith path ".js") (load-text-asset public-dir path)
   (.endsWith path ".html") (load-text-asset public-dir path)
   :else (create-binary-asset public-dir path)))

(defn- load-asset-and-refs [public-dir path]
  (let [asset (load-asset public-dir path)]
    (concat [asset] (mapcat #(load-asset-and-refs public-dir %) (:references asset)))))

(defn slice-path-to-after [public-dir #^String s]
  (subs s (+ (count public-dir)
             (.indexOf s (str public-dir "/")))))

(defn realize-regex-paths [public-dir path]
  (if (instance? java.util.regex.Pattern path)
    (let [paths (->> (file-paths-on-class-path)
                     (filter (fn [#^String p] (.contains p public-dir)))
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

;; load-bundles

(defn load-bundle [public-dir bundle paths]
  (let [paths (mapcat #(realize-regex-paths public-dir %) paths)
        assets (load-assets public-dir paths)
        path-set (set paths)
        set-bundle-for-original-files (fn [asset] (if (contains? path-set (:path asset))
                                                    (assoc asset :bundle bundle)
                                                    asset))]
    (map set-bundle-for-original-files assets)))

(defn load-bundles [public-dir bundles]
  (mapcat (fn [[bundle paths]]
            (load-bundle public-dir bundle paths))
          bundles))

;; with-prefix

(defn with-prefix
  [prefix & paths]
  (map (partial str prefix)
       (apply concat paths)))
