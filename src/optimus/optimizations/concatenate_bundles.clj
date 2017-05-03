(ns optimus.optimizations.concatenate-bundles
  (:require [clojure.set :refer [union]]
            [clojure.string :as str]
            [optimus.homeless :refer [assoc-non-nil max?]]))

(defn- concatenate-bundle [prefix [name assets]]
  (when name
    (-> {:path (str prefix "/" name)
         :contents (str/join "\n" (map :contents assets))
         :bundle name}
        (assoc-non-nil :references (apply union (map :references assets)))
        (assoc-non-nil :last-modified (max? (keep :last-modified assets))))))

(defn- mark-as-bundled [asset]
  (if (:bundle asset)
    (-> asset
        (dissoc :bundle)
        (assoc :bundled true))
    asset))

(defn concatenate-bundles
  ([assets] (concatenate-bundles assets {}))
  ([assets config]
   (let [prefix (or (:bundle-url-prefix config) "/bundles")]
     (concat (map mark-as-bundled assets)
             (keep #(concatenate-bundle prefix %) (group-by :bundle assets))))))
