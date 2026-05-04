(ns optimus.optimizations.minify
  (:require [clojure.string :as str]
            [optimus.clean-css :as clean-css]
            [optimus.csso :as csso]
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

(defn get-css-minifier [options]
  (if-let [clean-css (:clean-css options)]
    {:context (clean-css/create-context)
     :optimize #'clean-css/minify-css
     :options clean-css}
    {:context (csso/create-context)
     :optimize #'csso/minify
     :options (:csso options)}))

(defn minify-css [optimizer css {:keys [path]}]
  (if (looks-like-already-minified css)
    css
    (let [{:keys [optimize context options]} optimizer]
      (try
        (optimize context css options)
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
     (with-open [_context (:context optimizer)]
       (mapv #(minify-css-asset optimizer %) assets)))))
