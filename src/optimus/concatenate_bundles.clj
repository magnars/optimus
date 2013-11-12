(ns optimus.concatenate-bundles
  (:require [clojure.set :refer [union]]
            [clojure.string :as str]))

(defn- concatenate-bundle [[name files]]
  (when name
    {:path (str "/bundles/" name)
     :contents (str/join "\n" (map :contents files))
     :references (apply union (map :references files))
     :bundle name}))

(defn concatenate-bundles [files]
  (concat (map #(dissoc % :bundle) files)
          (keep concatenate-bundle (group-by :bundle files))))
