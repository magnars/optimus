(ns optimus.link
  (:require [optimus.assets :as assets]))

(defn full-path [asset]
  (if-let [base (:base-url asset)]
    (str base (:path asset))
    (:path asset)))

(defn file-path [request path]
  (->> request :optimus-assets
       (filter #(= path (assets/original-path %)))
       (remove :outdated)
       (first)
       full-path))

(defn- bundle-paths-1 [optimus-assets bundle]
  (->> optimus-assets
       (filter #(= bundle (:bundle %)))
       (remove :outdated)
       (map full-path)))

(defn bundle-paths [request bundles]
  (mapcat (partial bundle-paths-1 (:optimus-assets request)) bundles))
