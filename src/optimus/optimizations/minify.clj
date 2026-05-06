(ns optimus.optimizations.minify
  (:require [clojure.string :as str]
            [optimus.clean-css :as clean-css]
            [optimus.js :as js]
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
   (js/with-engine [engine (uglify-js/create-engine)]
     (minify-js engine js options)))
  ([engine js options]
   (if (looks-like-already-minified js)
     js
     (uglify-js/minify engine js (assoc (:uglify-js options) :path (:path options))))))

(defn minify-js-asset
  [engine asset options]
  (let [#^String path (:path asset)]
    (if (.endsWith path ".js")
      (update-in asset [:contents] #(minify-js engine % (assoc options :path path)))
      asset)))

(defn minify-js-assets
  ([assets] (minify-js-assets assets {}))
  ([assets options]
   (js/with-engine [engine (uglify-js/create-engine)]
     (mapv #(minify-js-asset engine % options) assets))))

;; minify CSS

(defn get-css-minifier [options]
  {:engine (clean-css/create-engine)
   :optimize #'clean-css/minify-css
   :options (:clean-css options)})

(defn minify-css [optimizer css {:keys [path]}]
  (if (looks-like-already-minified css)
    css
    (let [{:keys [optimize engine options]} optimizer]
      (try
        (optimize engine css options)
        (catch Exception e
          (throw (ex-info (str "Failed to optimize " path)
                          {:path path :options options}
                          e)))))))

(defn minify-css-asset
  [optimizer asset]
  (let [#^String path (:path asset)]
    (if (.endsWith path ".css")
      (update-in asset [:contents] #(minify-css optimizer % {:path path}))
      asset)))

(defn minify-css-assets
  ([assets] (minify-css-assets assets {}))
  ([assets options]
   (let [optimizer (get-css-minifier options)]
     (js/with-engine [_engine (:engine optimizer)]
       (mapv #(minify-css-asset optimizer %) assets)))))
