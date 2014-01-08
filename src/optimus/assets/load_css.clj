(ns optimus.assets.load-css
  (:require [optimus.assets.creation :refer [create-asset existing-resource original-path last-modified]]
            [pathetic.core :as pathetic]
            [clojure.string :as str]))

(defn- just-the-path [url]
  (-> url
      pathetic/parse-path
      pathetic/up-dir
      pathetic/render-path
      pathetic/ensure-trailing-separator))

(defn- to-absolute-url [container-url relative-url]
  (-> container-url
      (just-the-path)
      (pathetic/resolve relative-url)
      (pathetic/normalize)))

(def css-url-re #"(?:url\(['\"]?([^\)]+?)['\"]?\)|@import ['\"](.+?)['\"])")

(defn- data-url? [#^String url]
  (.startsWith url "data:"))

(defn- external-url? [#^String url]
  (re-matches #"^(?://|http://|https://).*" url))

(defn- url-match [[match & urls]]
  (first (remove nil? urls)))

(defn- match-url-to-absolute [original-path [match :as matches]]
  (let [url (url-match matches)]
    (if (or (data-url? url)
            (external-url? url))
      match ;; leave alone
      (str/replace match url (to-absolute-url original-path url)))))

(defn- make-css-urls-absolute [file]
  (->> (partial match-url-to-absolute (original-path file))
       (str/replace (:contents file) css-url-re)
       (assoc-in file [:contents])))

(defn- remove-url-appendages [s]
  (first (str/split s #"[\?#]")))

(defn- paths-in-css [file]
  (->> file :contents
       (re-seq css-url-re)
       (map url-match)
       (map remove-url-appendages)
       (remove data-url?)
       (remove external-url?)))

(defn create-css-asset [path contents last-modified]
  (let [asset (-> (create-asset path contents
                                :last-modified last-modified)
                  (make-css-urls-absolute))]
    (assoc asset :references (set (paths-in-css asset)))))

(defn load-css-asset [public-dir path]
  (let [resource (existing-resource public-dir path)]
    (create-css-asset path (slurp resource) (last-modified resource))))
