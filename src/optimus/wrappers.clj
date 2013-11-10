(ns optimus.wrappers
  (:require [clj-time.core :as time]
            [clj-time.format]
            [optimus.digest :as digest]
            [optimus.files :refer [->file replace-css-urls]]
            [clojure.set :refer [intersection difference]]))

;; wrap with files

(defn- ->bundled-files [bundle public-dir files]
  (->> files
       (map #(->file public-dir %))
       (map #(assoc % :bundle bundle))))

(defn- concat-files [request files]
  (update-in request [:optimus-files] #(concat % (doall files))))

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

(defn wrap-with-referenced-files [app public-dir]
  (fn [request]
    (let [files (->>  request :optimus-files
                      (mapcat :references)
                      (map #(->file public-dir %)))]
      (app (concat-files request files)))))

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

(defn- by-path [path files]
  (first (filter #(= path (:path %)) files)))

(defn- replace-css-urls-with-new-ones [file files]
  (let [orig->curr (into {} (map (juxt :original-path :path) files))]
    (-> file
        (replace-css-urls (fn [_ url] (get orig->curr url url)))
        (update-in [:references] (fn [refs] (when refs (set (replace orig->curr refs))))))))

(defn- add-cache-busted-expires-headers-in-order [to-replace files]
  ;; three cases:

  ;; 1. nothing more to replace? return the files
  (if (empty? to-replace)
    files

    ;; 2. are there files referenced by this file that are yet to be fixed?
    (let [next (by-path (first to-replace) files)
          remaining-references (intersection to-replace (:references next))]

    ;;    -> then take those first
      (if (seq remaining-references)
        (recur (concat remaining-references
                       (difference to-replace (:references next)))
               files)

        ;; 3. otherwise update all references in this file, and fix it too.
        ;;    then continue with the rest that remain.
        (->> files
             (replace {next (-> next
                                (replace-css-urls-with-new-ones files)
                                (add-cache-busted-expires-header))})
             (recur (set (rest to-replace))))))))

(defn- add-cache-busted-expires-headers [files]
  (let [cache-busted-files (add-cache-busted-expires-headers-in-order (set (map :path files)) files)]
    (concat
     (map #(assoc % :outdated true) files)
     cache-busted-files)))

(defn wrap-with-cache-busted-expires-headers [app]
  (fn [request]
    (app (update-in request [:optimus-files] add-cache-busted-expires-headers))))
