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

(defn concatenate-bundles [assets]
  (concat (map #(dissoc % :bundle) assets)
          (keep concatenate-bundle (group-by :bundle assets))))
