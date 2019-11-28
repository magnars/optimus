(ns optimus.optimizations.add-last-modified-headers
  (:require [optimus.time :as time]))

(defn- add-last-modified-headers-1
  [asset]
  (if (:last-modified asset)
    (assoc-in asset [:headers "Last-Modified"]
              (time/format-http-date
               (time/from-millis (:last-modified asset))))
    asset))

(defn add-last-modified-headers
  [assets]
  (map add-last-modified-headers-1 assets))
