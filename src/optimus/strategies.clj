(ns optimus.strategies
  (:require [clojure.core.memoize :as memo]
            [clojure.java.io :as io]
            [juxt.dirwatch :refer [watch-dir]]
            [optimus.asset :as asset]
            [optimus.homeless :refer [assoc-non-nil jdk-version]]))

(defn- serve-asset [asset]
  (-> {:status 200 :body (or (:contents asset)
                             (io/input-stream (:resource asset)))}
      (assoc-non-nil :headers (:headers asset))))

(defn- serve-asset-or-continue [assets path->asset app request respond raise]
  (if-let [asset (path->asset (:uri request))]
    (respond (serve-asset asset))
    (app (assoc request :optimus-assets assets) respond raise)))

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

(defn- async-shim [f handler req]
  (let [result (atom nil)]
    (->> (fn [req respond raise]
           (respond (handler req)))
         (f req #(reset! result %) #(throw %)))
    @result))

(defn serve-live-assets [app get-assets optimize options]
  (let [get-optimized-assets #(optimize (guard-against-duplicate-assets (get-assets)) options)
        get-optimized-assets (if-let [ms (get options :cache-live-assets 2000)]
                               (memo/ttl get-optimized-assets {} :ttl/threshold ms)
                               get-optimized-assets)]
    (fn live-assets-handler
      ([request]
       (async-shim live-assets-handler app request))
      ([request respond raise]
       (live-assets-handler request respond raise app))
      ([request respond raise app-handler]
       (let [assets (get-optimized-assets)
             path->asset (into {} (map (juxt asset/path identity) assets))]
         (serve-asset-or-continue assets path->asset app-handler request respond raise))))))

(defn serve-live-assets-autorefresh [app get-assets optimize options]
  (let [get-optimized-assets #(optimize (guard-against-duplicate-assets (get-assets)) options)
        assets-dir (clojure.java.io/file (get options :assets-dir "resources"))
        assets-cache (atom (get-optimized-assets))
        on-assets-changed (fn [change]
                            (when-not (.isDirectory (:file change))
                              (reset! assets-cache (get-optimized-assets))))]
    (watch-dir on-assets-changed assets-dir)
    (fn live-assets-autorefresh-handler
      ([request]
       (async-shim live-assets-autorefresh-handler app request))
      ([request respond raise]
       (live-assets-autorefresh-handler request respond raise app))
      ([request respond raise app-handler]
       (let [assets @assets-cache
             path->asset (into {} (map (juxt asset/path identity) assets))]
         (serve-asset-or-continue assets path->asset app-handler request respond raise))))))

(defn serve-frozen-assets [app get-assets optimize options]
  (let [assets (optimize (guard-against-duplicate-assets (get-assets)) options)
        path->asset (into {} (map (juxt asset/path identity) assets))]
    (fn frozen-assets-handler
      ([request]
       (async-shim frozen-assets-handler app request))
      ([request respond raise]
       (frozen-assets-handler request respond raise app))
      ([request respond raise app-handler]
       (serve-asset-or-continue assets path->asset app-handler request respond raise)))))
