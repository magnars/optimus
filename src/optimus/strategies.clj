(ns optimus.strategies
  (:require [optimus.homeless :refer [assoc-non-nil jdk-version]]
            [clojure.java.io :as io]
            [clojure.core.memoize :as memo]))

(defn- serve-asset [asset]
  (-> {:status 200 :body (or (:contents asset)
                             (io/input-stream (:resource asset)))}
      (assoc-non-nil :headers (:headers asset))))

(defn- serve-asset-or-continue [assets path->asset app request]
  (if-let [asset (path->asset (:uri request))]
    (serve-asset asset)
    (app (assoc request :optimus-assets assets))))

(defn- collapse-equal-assets [asset-1 asset-2]
  (when-not (= asset-1 asset-2)
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

(defn serve-live-assets-autorefresh [app get-assets optimize options]
  (if (< (jdk-version) 1.7)
    (throw (Exception. "This strategy is not supported on JDK version < 1.7"))
    (do
      (use '[juxt.dirwatch :only [watch-dir]])
      (let [get-optimized-assets #(optimize (guard-against-duplicate-assets (get-assets)) options)
            assets-dir (clojure.java.io/file (get options :assets-dir "resources"))
            assets-cache (atom (get-optimized-assets))
            on-assets-changed (fn [change]
                                (if-not (.isDirectory (:file change))
                                  (reset! assets-cache (get-optimized-assets))))]
        (@(resolve 'watch-dir) on-assets-changed assets-dir)
        (fn [request]
          (let [assets @assets-cache
                path->asset (into {} (map (juxt :path identity) assets))]
            (serve-asset-or-continue assets path->asset app request)))))))

(defn serve-frozen-assets [app get-assets optimize options]
  (let [assets (optimize (guard-against-duplicate-assets (get-assets)) options)
        path->asset (into {} (map (juxt :path identity) assets))]
    (fn [request]
      (serve-asset-or-continue assets path->asset app request))))
