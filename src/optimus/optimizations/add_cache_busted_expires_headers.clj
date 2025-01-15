(ns optimus.optimizations.add-cache-busted-expires-headers
  (:require [cemerick.url :as url]
            [clojure.set :as set]
            [clojure.string :as str]
            [optimus.assets :refer [original-path]]
            [optimus.digest :as digest]
            [optimus.paths :as paths]
            [optimus.time :as time])
  (:import [java.time ZonedDateTime ZoneId]
           [java.time.format DateTimeFormatter]))

(defn get-contents [file]
  (or (:contents file)
      (slurp (:resource file))))

(defn add-cache-busted-expires-header [file]
  (-> file
      (assoc :path (str (paths/just-the-path (:path file))
                        (subs (digest/sha-1 (get-contents file)) 0 12)
                        \/
                        (paths/just-the-filename (:path file))))
      (assoc :original-path (original-path file))
      (assoc-in [:headers "Cache-Control"] "max-age=315360000")
      (assoc-in [:headers "Expires"] (time/format-http-date
                                      (.plusYears (time/now) 10)))))

(defn by-path [path files]
  (first (filter #(= path (:path %)) files)))

(defn qualify-url [{:keys [base-url context-path path]}]
  (cond->> path
    context-path (str context-path)
    base-url (str (:path (url/url base-url)))))

(defn inverse-string-length [^String s]
  (- (.length s)))

(defn replace-referenced-url [file old new]
  (update-in file [:contents] #(str/replace % old new)))

(defn replace-referenced-urls [file old->new]
  (reduce #(replace-referenced-url %1 %2 (old->new %2)) file
          (sort-by inverse-string-length (:references file))))

(defn replace-referenced-urls-with-new-ones [file files]
  (if-let [references (:references file)]
    (let [orig->curr (into {} (map (juxt original-path qualify-url) files))]
      (-> file
          (replace-referenced-urls orig->curr)
          (assoc :references (set (replace orig->curr references)))))
    file))

(defn add-cache-busted-expires-headers-in-order [to-replace files]
  ;; three cases:

  ;; 1. nothing more to replace? return the files
  (if (empty? to-replace)
    files

    ;; 2. are there files referenced by this file that aren't fixed yet?
    (let [next (by-path (first to-replace) files)
          remaining-references (set/intersection to-replace (set (:references next)))]

      ;;    -> then take those first
      (if (seq remaining-references)
        (recur (concat remaining-references (set/difference to-replace remaining-references))
               files)

        ;; 3. otherwise update all references in this file, and fix it too.
        (->> files
             (replace {next (-> next
                                (replace-referenced-urls-with-new-ones files)
                                (add-cache-busted-expires-header))})

             ;; and continue with the rest that remain.
             (recur (set (rest to-replace))))))))

(defn add-cache-busted-expires-headers [files]
  (let [cache-busted-files (add-cache-busted-expires-headers-in-order (set (map :path files)) files)]
    (concat
     (map #(assoc % :outdated true) files)
     cache-busted-files)))
