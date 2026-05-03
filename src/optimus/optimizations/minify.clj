(ns optimus.optimizations.minify
  (:require [clojure.string :as str]
            [optimus.clean-css :as clean-css]
            [optimus.uglify-js :as uglify-js]))

(defn looks-like-already-minified
  "Files with a single line over 1000 characters are considered already
  minified, and skipped. This avoid issues with huge bootstrap.css files
  and its ilk."
  [contents]
  (->> contents
       (str/split-lines)
       (some (fn [^String s] (> (.length s) 1000)))))

(defn minify-js
  ([js] (minify-js js {}))
  ([js options]
   (with-open [context (uglify-js/create-context)]
     (minify-js context js options)))
  ([context js options]
   (if (looks-like-already-minified js)
     js
     (try
       (uglify-js/minify context js (:uglify-js options))
       (catch Exception e
         (throw (ex-info (str "Failed to minify " (:path options)) options e)))))))

(defn minify-js-asset
  [context asset options]
  (let [#^String path (:path asset)]
    (if (.endsWith path ".js")
      (update-in asset [:contents] #(minify-js context % (assoc options :path path)))
      asset)))

(defn minify-js-assets
  ([assets] (minify-js-assets assets {}))
  ([assets options]
   (with-open [context (uglify-js/create-context)]
     (mapv #(minify-js-asset context % options) assets))))

;; minify CSS

(defn minify-css
  ([css] (minify-css css {}))
  ([css options]
   (with-open [context (clean-css/create-context)]
     (minify-css context css options)))
  ([context css options]
   (if (looks-like-already-minified css)
     css
     (clean-css/minify-css context css (:clean-css options)))))

(defn minify-css-asset
  [context asset options]
  (let [#^String path (:path asset)]
    (if (.endsWith path ".css")
      (update-in asset [:contents] #(minify-css context % (assoc options :path path)))
      asset)))

(defn minify-css-assets
  ([assets] (minify-css-assets assets {}))
  ([assets options]
   (with-open [context (clean-css/create-context)]
     (mapv #(minify-css-asset context % options) assets))))
