(ns optimus.strategies)

(defn- assoc-non-nil [map key val]
  (if val
    (assoc map key val)
    map))

(defn- serve [asset]
  (-> {:status 200 :body (:contents asset)}
      (assoc-non-nil :headers (:headers asset))))

(defn serve-unchanged-assets [app get-assets]
  (fn [request]
    (let [assets (get-assets)
          path->asset (into {} (map (juxt :path identity) assets))]
      (if-let [asset (path->asset (:uri request))]
        (serve asset)
        (app (assoc request :optimus-assets assets))))))
