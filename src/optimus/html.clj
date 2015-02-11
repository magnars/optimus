(ns optimus.html
  (:require [clojure.string :as str]
            [optimus.link :as link]))

(defn link-to-js-bundles [request bundles]
  (->> (link/bundle-paths request bundles)
       (map (fn [path] (str "<script src=\"" path "\"></script>")))
       (str/join)))

(defn link-to-css-bundles [request bundles]
  (->> (link/bundle-paths request bundles)
       (map (fn [path] (str "<link href=\"" path "\" rel=\"stylesheet\" />")))
       (str/join)))
