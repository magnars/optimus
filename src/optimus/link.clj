(ns optimus.link
  (:require [optimus.assets :as assets]))

(defn file-path [request path]
  (->> request :optimus-assets
       (filter #(= path (assets/original-path %)))
       (remove :outdated)
       (first)
       :path))

(defn- bundle-paths-1 [optimus-assets bundle]
  (->> optimus-assets
       (filter #(= bundle (:bundle %)))
       (remove :outdated)
       (map :path)))

(defn bundle-paths [request bundles]
  (mapcat (partial bundle-paths-1 (:optimus-assets request)) bundles))
