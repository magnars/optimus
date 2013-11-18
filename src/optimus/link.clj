(ns optimus.link
  (:require [optimus.assets :as assets]))

(defn file-path [request path]
  (->> request :optimus-assets
       (filter #(= path (assets/original-path %)))
       (remove :outdated)
       (first)
       :path))
