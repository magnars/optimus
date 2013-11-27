(ns optimus.optimizations
  (:require [optimus.optimizations.add-cache-busted-expires-headers]
            [optimus.optimizations.concatenate-bundles]
            [optimus.optimizations.minify]))

(def minify-js-assets optimus.optimizations.minify/minify-js-assets)
(def minify-css-assets optimus.optimizations.minify/minify-css-assets)
(def concatenate-bundles optimus.optimizations.concatenate-bundles/concatenate-bundles)
(def add-cache-busted-expires-headers optimus.optimizations.add-cache-busted-expires-headers/add-cache-busted-expires-headers)

(defn all [assets options]
  (-> assets
      (minify-js-assets options)
      (minify-css-assets options)
      (concatenate-bundles)
      (add-cache-busted-expires-headers)))

(defn none [assets options] assets)

