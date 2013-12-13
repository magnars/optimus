(ns optimus.assets.load-css
  (:require [optimus.assets.creation :refer [load-asset create-asset existing-resource original-path]]
            [pathetic.core :as pathetic]
            [clojure.string :as str]))

(defn- just-the-path [url]
  (-> url
      pathetic/parse-path
      pathetic/up-dir
      pathetic/render-path
      pathetic/ensure-trailing-separator))

(defn- remove-url-appendages [s]
  (first (str/split s #"[\?#]")))

(defn- combine-paths [container-url relative-url]
  (-> container-url
      (just-the-path)
      (pathetic/resolve (remove-url-appendages relative-url))
      (pathetic/normalize)))

(def css-url-re #"url\(['\"]?([^\)]+?)['\"]?\)")

(defn- css-url-str [url]
  (str "url('" url "')"))

(defn- data-url? [#^String url]
  (.startsWith url "data:"))

(defn- external-url? [#^String url]
  (re-matches #"^(?://|http://|https://).*" url))

(defn- replace-css-urls [file replacement-fn]
  (assoc-in file [:contents]
            (str/replace (:contents file) css-url-re
                         (fn [[match url]] (if (or (data-url? url) (external-url? url))
                                             match
                                             (css-url-str (replacement-fn file url)))))))

(defn paths-in-css [file]
  (->> file :contents
       (re-seq css-url-re)
       (map second)
       (remove data-url?)
       (remove external-url?)
       (map #(combine-paths (original-path file) %))))

(defn load-css-asset [public-dir path]
  (let [contents (slurp (existing-resource public-dir path))
        asset (-> (create-asset path contents)
                  (replace-css-urls #(combine-paths (original-path %1) %2)))]
    (assoc asset :references (set (paths-in-css asset)))))
