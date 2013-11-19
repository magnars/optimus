(ns optimus.add-cache-busted-expires-headers
  (:require [clj-time.core :as time]
            [clj-time.format]
            [optimus.digest :as digest]
            [optimus.assets :refer [replace-css-urls original-path]]
            [optimus.homeless :refer [assoc-non-nil]]
            [clojure.set :refer [intersection difference]]))

(def http-date-format
  (clj-time.format/formatter "EEE, dd MMM yyyy HH:mm:ss 'GMT'"))

(def http-date-formatter (partial clj-time.format/unparse http-date-format))

(defn- add-cache-busted-expires-header [file]
  (-> file
      (assoc :path (str "/"
                        (subs (digest/sha-1 (:contents file)) 0 12)
                        (:path file)))
      (assoc :original-path (original-path file))
      (assoc :headers {"Cache-Control" "max-age=315360000" ;; 3650 days
                       "Expires" (http-date-formatter (time/plus (time/now)
                                                                 (time/days 3650)))})))

(defn- by-path [path files]
  (first (filter #(= path (:path %)) files)))

(defn- replace-css-urls-with-new-ones [file files]
  (let [orig->curr (into {} (map (juxt original-path :path) files))]
    (-> file
        (replace-css-urls (fn [_ url] (get orig->curr url url)))
        (assoc-non-nil :references (when (:references file)
                                     (set (replace orig->curr (:references file))))))))

(defn- add-cache-busted-expires-headers-in-order [to-replace files]
  ;; three cases:

  ;; 1. nothing more to replace? return the files
  (if (empty? to-replace)
    files

    ;; 2. are there files referenced by this file that aren't fixed yet?
    (let [next (by-path (first to-replace) files)
          remaining-references (intersection to-replace (:references next))]

      ;;    -> then take those first
      (if (seq remaining-references)
        (recur (concat remaining-references (difference to-replace remaining-references))
               files)

        ;; 3. otherwise update all references in this file, and fix it too.
        (->> files
             (replace {next (-> next
                                (replace-css-urls-with-new-ones files)
                                (add-cache-busted-expires-header))})

             ;; and continue with the rest that remain.
             (recur (set (rest to-replace))))))))

(defn add-cache-busted-expires-headers [files]
  (let [cache-busted-files (add-cache-busted-expires-headers-in-order (set (map :path files)) files)]
    (concat
     (map #(assoc % :outdated true) files)
     cache-busted-files)))
