(ns optimus.optimizations.add-last-modified-headers
  (:require [clj-time.coerce]
            [clj-time.format]))

(def http-date-format
  (clj-time.format/formatter "EEE, dd MMM yyyy HH:mm:ss 'GMT'"))

(def http-date-formatter (partial clj-time.format/unparse http-date-format))

(defn- add-last-modified-headers-1
  [asset]
  (if (:last-modified asset)
    (assoc-in asset [:headers "Last-Modified"]
              (http-date-formatter (clj-time.coerce/from-long (:last-modified asset))))
    asset))

(defn add-last-modified-headers
  [assets]
  (map add-last-modified-headers-1 assets))
