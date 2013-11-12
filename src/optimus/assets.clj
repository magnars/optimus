(ns optimus.assets
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [pathetic.core :as pathetic])
  (:import java.io.FileNotFoundException))

;; create-asset

(defn guard-path [path]
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

(defn- existing-resource [public-dir path]
  (or (io/resource (str public-dir path))
      (throw (FileNotFoundException. path))))

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

(defn replace-css-urls [file replacement-fn]
  (assoc-in file [:contents]
            (str/replace (:contents file) css-url-re
                         (fn [[_ url]] (css-url-str (replacement-fn file url))))))

(defn paths-in-css [file]
  (->> file :contents
       (re-seq css-url-re)
       (map #(combine-paths (original-path file) (second %)))))

(defn- load-css-asset [public-dir path]
  (let [contents (slurp (existing-resource public-dir path))
        asset (-> (create-asset path contents)
                  (replace-css-urls #(combine-paths (original-path %1) %2)))]
    (assoc asset :references (set (paths-in-css asset)))))

;; load-assets

(defn- load-asset [public-dir path]
  (guard-path path)
  (if (.endsWith path ".css")
    (load-css-asset public-dir path)
    (create-asset path (slurp (existing-resource public-dir path)))))

(defn- load-asset-and-refs [public-dir path]
  (let [asset (load-asset public-dir path)]
    (concat [asset] (mapcat #(load-asset-and-refs public-dir %) (:references asset)))))

(defn load-assets [public-dir paths]
  (mapcat #(load-asset-and-refs public-dir %) paths))

;; load-bundles

(defn load-bundle [public-dir bundle paths]
  (let [assets (load-assets public-dir paths)
        paths (set paths)
        set-bundle-for-original-files (fn [asset] (if (contains? paths (:path asset))
                                                    (assoc asset :bundle bundle)
                                                    asset))]
    (map set-bundle-for-original-files assets)))

(defn load-bundles [public-dir bundles]
  (mapcat (fn [[bundle paths]]
            (load-bundle public-dir bundle paths))
          bundles))
