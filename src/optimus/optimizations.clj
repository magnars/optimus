(ns optimus.optimizations
  (:require [optimus.optimizations.add-cache-busted-expires-headers]
            [optimus.optimizations.concatenate-bundles]
            [optimus.optimizations.minify]
            [optimus.optimizations.add-last-modified-headers]
            [optimus.optimizations.inline-css-imports]))

(def minify-js-assets optimus.optimizations.minify/minify-js-assets)
(def minify-css-assets optimus.optimizations.minify/minify-css-assets)
(def concatenate-bundles optimus.optimizations.concatenate-bundles/concatenate-bundles)
(def add-cache-busted-expires-headers optimus.optimizations.add-cache-busted-expires-headers/add-cache-busted-expires-headers)
(def add-last-modified-headers optimus.optimizations.add-last-modified-headers/add-last-modified-headers)
(def inline-css-imports optimus.optimizations.inline-css-imports/inline-css-imports)

(defn all [assets options]
  (-> assets
      (minify-js-assets options)
      (minify-css-assets options)
      (inline-css-imports)
      (concatenate-bundles)
      (add-cache-busted-expires-headers)
      (add-last-modified-headers)))

(defn none [assets options] assets)

