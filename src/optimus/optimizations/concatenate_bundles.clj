(ns optimus.optimizations.concatenate-bundles
  (:require [clojure.set :refer [union]]
            [clojure.string :as str]
            [optimus.homeless :refer [assoc-non-nil]]))

(defn- concatenate-bundle [[name assets]]
  (when name
    (-> {:path (str "/bundles/" name)
         :contents (str/join "\n" (map :contents assets))
         :bundle name}
        (assoc-non-nil :references (apply union (map :references assets))))))

(defn- mark-as-bundled [asset]
  (-> asset
      (dissoc :bundle)
      (assoc :bundled true)))

(defn concatenate-bundles [assets]
  (concat (map mark-as-bundled assets)
          (keep concatenate-bundle (group-by :bundle assets))))
