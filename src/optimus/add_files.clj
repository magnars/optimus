(ns optimus.add-files
  (:require [optimus.files :refer [->file]]))

(defn- ->bundled-files [bundle public-dir files]
  (->> files
       (map #(->file public-dir %))
       (map #(assoc % :bundle bundle))))

(defn add-file-bundle [list bundle public-dir files]
  (concat list (doall (->bundled-files bundle public-dir files))))

(defn add-file-bundles [list public-dir bundles]
  (concat list (mapcat (fn [[bundle files]]
                         (->bundled-files bundle public-dir files))
                       bundles)))

(defn add-files [list public-dir files]
  (concat list (map #(->file public-dir %) files)))

(defn add-referenced-files [list public-dir]
  (concat list (->> list
                    (mapcat :references)
                    (map #(->file public-dir %)))))
