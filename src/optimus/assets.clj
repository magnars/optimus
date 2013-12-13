(ns optimus.assets
  (:require [optimus.assets.creation :refer [load-asset]]
            [optimus.assets.load-css]))

(defmethod load-asset "css"
  [public-dir path]
  (optimus.assets.load-css/load-css-asset public-dir path))

(def create-asset optimus.assets.creation/create-asset)
(def load-assets optimus.assets.creation/load-assets)
(def load-bundle optimus.assets.creation/load-bundle)
(def load-bundles optimus.assets.creation/load-bundles)

(def original-path optimus.assets.creation/original-path)

(defn with-prefix
  [prefix & paths]
  (map (partial str prefix)
       (apply concat paths)))
