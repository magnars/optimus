(ns optimus.link
  (:require [optimus.assets :as assets]))

(defn full-path [{:keys [base-url context-path path]}]
  (str base-url context-path path))

(defn file-path [request path & {:as options}]
  (let [asset (assets/get-asset-by-path request path)]
    (if asset
      (full-path asset)
      (when-let [fallback (:fallback options)]
        (file-path request fallback)))))

(defn bundle-paths-1 [optimus-assets bundle]
  (->> optimus-assets
       (filter #(= bundle (:bundle %)))
       (remove :outdated)
       (map full-path)))

(defn bundle-paths [request bundles]
  (mapcat (partial bundle-paths-1 (:optimus-assets request)) bundles))
