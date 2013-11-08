(ns optimus.files
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [pathetic.core :as pathetic])
  (:import java.io.FileNotFoundException))

(defn- file-struct [path type contents]
  {:path path
   :original-path path
   :contents contents})

(defn- existing-resource [public-dir path]
  (or (io/resource (str public-dir path))
      (throw (FileNotFoundException. path))))

;; binary

(defn- binary-file [public-dir path]
  (let [resource (existing-resource public-dir path)]
    (file-struct path :binary (io/input-stream resource))))

;; css

(defn- just-the-path [url]
  (-> url
      pathetic/parse-path
      pathetic/up-dir
      pathetic/render-path
      pathetic/ensure-trailing-separator))

(defn- remove-url-appendages [s]
  (first (str/split s #"[\?#]")))

(defn- combine-paths [container-url relative-url]
  (pathetic/normalize (pathetic/resolve (just-the-path container-url)
                                        (remove-url-appendages relative-url))))

(def css-url-re #"url\(['\"]?([^\)]+?)['\"]?\)")

(defn- css-url-str [url]
  (str "url('" url "')"))

(defn- replace-css-urls [file replacement-fn]
  (assoc-in file [:contents]
   (str/replace (:contents file) css-url-re
                (fn [[_ url]] (css-url-str (replacement-fn file url))))))

(defn- css-file [public-dir path]
  (let [resource (existing-resource public-dir path)]
    (-> (file-struct path :css (slurp resource))
        (replace-css-urls #(combine-paths (:original-path %1) %2)))))

;; js

(defn- js-file [public-dir path]
  (let [resource (existing-resource public-dir path)]
    (file-struct path :js (slurp resource))))

;; public api

(defn ->files [public-dir path]
  (when-not (.startsWith path "/")
    (throw (Exception. (str "File paths must start with a slash. Got: " path))))
  (cond
   (.endsWith path ".css") [(css-file public-dir path)]
   (.endsWith path ".js") [(js-file public-dir path)]
   :else [(binary-file public-dir path)]))
