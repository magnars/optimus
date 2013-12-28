(ns optimus.strategies
  (:require [optimus.homeless :refer [assoc-non-nil]]
            [clojure.core.memoize :as memo]))

(defn- serve-asset [asset]
  (-> {:status 200 :body (or (:contents asset)
                             ((:get-stream asset)))}
      (assoc-non-nil :headers (:headers asset))))

(defn- serve-asset-or-continue [assets path->asset app request]
  (if-let [asset (path->asset (:uri request))]
    (serve-asset asset)
    (app (assoc request :optimus-assets assets))))

(defn- collapse-equal-assets [asset-1 asset-2]
  (when-not (= (dissoc asset-1 :get-stream)
               (dissoc asset-2 :get-stream))
    (throw (Exception. (str "Two assets have the same path \"" (:path asset-1) "\", but are not equal."))))
  asset-1)

(defn- guard-against-duplicate-assets [assets]
  (let [pb->assets (group-by (juxt :path :bundle) assets)]
    (->> assets
         (map (juxt :path :bundle))
         (distinct)
         (map pb->assets)
         (map #(reduce collapse-equal-assets %)))))

(defn serve-live-assets [app get-assets optimize options]
  (let [get-optimized-assets #(optimize (guard-against-duplicate-assets (get-assets)) options)
        get-optimized-assets (if-let [ms (get options :cache-live-assets 2000)]
                               (memo/ttl get-optimized-assets {} :ttl/threshold ms)
                               get-optimized-assets)]
    (fn [request]
      (let [assets (get-optimized-assets)
            path->asset (into {} (map (juxt :path identity) assets))]
        (serve-asset-or-continue assets path->asset app request)))))

(defn serve-frozen-assets [app get-assets optimize options]
  (let [assets (optimize (guard-against-duplicate-assets (get-assets)) options)
        path->asset (into {} (map (juxt :path identity) assets))]
    (fn [request]
      (serve-asset-or-continue assets path->asset app request))))
