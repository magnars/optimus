(ns optimus.strategies
  (:require [optimus.concatenate-bundles :refer [concatenate-bundles]]
            [optimus.add-cache-busted-expires-headers :refer [add-cache-busted-expires-headers]]
            [optimus.homeless :refer [assoc-non-nil]]))

(defn- serve-asset [asset]
  (-> {:status 200 :body (:contents asset)}
      (assoc-non-nil :headers (:headers asset))))

(defn- serve-asset-or-continue [assets path->asset app request]
  (if-let [asset (path->asset (:uri request))]
    (serve-asset asset)
    (app (assoc request :optimus-assets assets))))

(defn- serve-live-assets [app get-assets]
  (fn [request]
    (let [assets (get-assets)
          path->asset (into {} (map (juxt :path identity) assets))]
      (serve-asset-or-continue assets path->asset app request))))

(defn- serve-frozen-assets [app get-assets]
  (let [assets (get-assets)
        path->asset (into {} (map (juxt :path identity) assets))]
    (fn [request]
      (serve-asset-or-continue assets path->asset app request))))

(defn- optimize-assets [assets]
  (-> assets
      concatenate-bundles
      add-cache-busted-expires-headers))

(defn serve-unchanged-assets [app get-assets]
  (serve-live-assets app get-assets))

(defn serve-optimized-assets [app get-assets]
  (serve-live-assets app #(optimize-assets (get-assets))))

(defn serve-frozen-optimized-assets [app get-assets]
  (serve-frozen-assets app #(optimize-assets (get-assets))))
