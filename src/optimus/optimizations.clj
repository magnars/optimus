(ns optimus.optimizations
  (:require [optimus.optimizations.add-cache-busted-expires-headers]
            [optimus.optimizations.concatenate-bundles]
            [optimus.optimizations.minify]
            [optimus.optimizations.add-last-modified-headers]
            [optimus.optimizations.inline-css-imports]
            [potemkin :refer [import-vars]]))

(import-vars [optimus.optimizations.minify
              minify-js-assets
              minify-css-assets]
             [optimus.optimizations.concatenate-bundles
              concatenate-bundles]
             [optimus.optimizations.add-cache-busted-expires-headers
              add-cache-busted-expires-headers]
             [optimus.optimizations.add-last-modified-headers
              add-last-modified-headers]
             [optimus.optimizations.inline-css-imports
              inline-css-imports])

(defn all [assets options]
  (-> assets
      (minify-js-assets options)
      (minify-css-assets options)
      (inline-css-imports)
      (concatenate-bundles options)
      (add-cache-busted-expires-headers)
      (add-last-modified-headers)))

(defn none [assets options] assets)

