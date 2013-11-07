(ns optimus.wrappers
  (:require [clojure.java.io :as io]
            [clj-time.core :as time]
            [clj-time.format]
            [optimus.digest :as digest]))

;; wrap with files

(defn ->file [public-dir file]
  {:path file
   :original-path file
   :contents (slurp (io/resource (str public-dir file)))})

(defn- ->bundled-files [bundle public-dir files]
  (map #(assoc (->file public-dir %) :bundle bundle) files))

(defn- concat-files [request files]
  (update-in request [:optimus-files] concat (doall files)))

(defn wrap-with-file-bundle [app bundle public-dir files]
  (fn [request]
    (app (concat-files request (->bundled-files bundle public-dir files)))))

(defn wrap-with-file-bundles [app public-dir bundles]
  (fn [request]
    (let [bundle-files (mapcat (fn [[bundle files]] (->bundled-files bundle public-dir files)) bundles)]
      (app (concat-files request bundle-files)))))

(defn wrap-with-files [app public-dir files]
  (fn [request]
    (app (concat-files request (map #(->file public-dir %) files)))))

;; cache-busters and expired headers

(def http-date-format
  (clj-time.format/formatter "EEE, dd MMM yyyy HH:mm:ss 'GMT'"))

(def http-date-formatter (partial clj-time.format/unparse http-date-format))

(defn- add-cache-busted-expires-header [file]
  (-> file
      (assoc :path (str "/"
                        (subs (digest/sha-1 (:contents file)) 0 12)
                        (:path file)))
      (assoc :headers {"Cache-Control" "max-age=315360000" ;; 3650 days
                       "Expires" (http-date-formatter (time/plus (time/now)
                                                                 (time/days 3650)))})))

(defn- add-cache-busted-expires-headers [files]
  (concat
   (map #(assoc % :outdated true) files)
   (map add-cache-busted-expires-header files)))

(defn wrap-with-cache-busted-expires-headers [app]
  (fn [request]
    (app (update-in request [:optimus-files] add-cache-busted-expires-headers))))
