(ns optimus.hiccup
  (:require [optimus.link :as link]))

(defn link-to-js-bundles [request bundles]
  (map (fn [path] [:script {:src path}])
       (link/bundle-paths request bundles)))

(defn link-to-css-bundles [request bundles]
  (map (fn [path] [:link {:rel "stylesheet" :href path}])
       (link/bundle-paths request bundles)))
