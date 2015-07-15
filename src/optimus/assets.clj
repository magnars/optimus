(ns optimus.assets
  (:require [optimus.assets.creation :refer [load-asset]]
            [optimus.assets.load-css]
            [potemkin :refer [import-vars]]))

(defmethod load-asset "js"   [public-dir path] (optimus.assets.creation/load-text-asset public-dir path))
(defmethod load-asset "html" [public-dir path] (optimus.assets.creation/load-text-asset public-dir path))
(defmethod load-asset "css"  [public-dir path] (optimus.assets.load-css/load-css-asset public-dir path))

(import-vars [optimus.assets.creation
              create-asset
              load-assets
              load-bundle
              load-bundles
              original-path])

(defn with-prefix
  [prefix & paths]
  (map (partial str prefix)
       (apply concat paths)))
