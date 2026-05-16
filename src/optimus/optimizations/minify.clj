(ns optimus.optimizations.minify
  (:require [clojure.string :as str]
            [optimus.clean-css :as clean-css]
            [optimus.csso :as csso]
            [optimus.js :as js]
            [optimus.ph-css :as ph-css]
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
  (or
   (when-let [clean-css (:clean-css options)]
     {:engine (clean-css/create-engine)
      :optimize #'clean-css/minify-css
      :options clean-css})
   (when-let [csso (:csso options)]
     {:engine (csso/create-engine)
      :optimize #'csso/minify
      :options csso})
   {:optimize #'ph-css/minify
    :options (:ph-css options)}))

(defn minify-css [optimizer css {:keys [path]}]
  (if (looks-like-already-minified css)
    css
    (let [{:keys [optimize engine options]} optimizer]
      (try
        (if engine
          (optimize engine css options)
          (optimize css options))
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
     (if (:engine optimizer)
       (js/with-engine [_engine (:engine optimizer)]
         (mapv #(minify-css-asset optimizer %) assets))
       (mapv #(minify-css-asset optimizer %) assets)))))
