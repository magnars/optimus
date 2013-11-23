(ns optimus.strategies
  (:require [optimus.concatenate-bundles :refer [concatenate-bundles]]
            [optimus.add-cache-busted-expires-headers :refer [add-cache-busted-expires-headers]]
            [optimus.minify :refer [minify-js-assets minify-css-assets]]
            [optimus.homeless :refer [assoc-non-nil]]
            [clojure.core.memoize :as memo]))

(defn- serve-asset [asset]
  (-> {:status 200 :body (:contents asset)}
      (assoc-non-nil :headers (:headers asset))))

(defn- serve-asset-or-continue [assets path->asset app request]
  (if-let [asset (path->asset (:uri request))]
    (serve-asset asset)
    (app (assoc request :optimus-assets assets))))

(defn- serve-live-assets [app get-assets options]
  (let [get-assets (if-let [ms (get options :cache-live-assets 2000)]
                     (memo/ttl get-assets {} :ttl/threshold ms)
                     get-assets)]
    (fn [request]
      (let [assets (get-assets)
            path->asset (into {} (map (juxt :path identity) assets))]
        (serve-asset-or-continue assets path->asset app request)))))

(defn- serve-frozen-assets [app get-assets options]
  (let [assets (get-assets)
        path->asset (into {} (map (juxt :path identity) assets))]
    (fn [request]
      (serve-asset-or-continue assets path->asset app request))))

(defn- optimize-assets [assets options]
  (-> assets
      (minify-js-assets options)
      (minify-css-assets options)
      (concatenate-bundles)
      (add-cache-busted-expires-headers)))

(defn serve-unchanged-assets
  ([app get-assets] (serve-unchanged-assets app get-assets {}))
  ([app get-assets options] (serve-live-assets app get-assets options)))

(defn serve-optimized-assets
  ([app get-assets] (serve-optimized-assets app get-assets {}))
  ([app get-assets options] (serve-live-assets app #(optimize-assets (get-assets) options) options)))

(defn serve-frozen-optimized-assets
  ([app get-assets] (serve-frozen-optimized-assets app get-assets {}))
  ([app get-assets options] (serve-frozen-assets app #(optimize-assets (get-assets) options) options)))
