(ns optimus.wrappers
  (:require [optimus.add-files :as add-files]
            [optimus.add-cache-busted-expires-headers :refer [add-cache-busted-expires-headers]]
            [optimus.concatenate-bundles :refer [concatenate-bundles]]))

(defn- update-files-handler [app f]
  (fn [request]
    (app (update-in request [:optimus-files] f))))

(defn wrap-with-file-bundle [app bundle public-dir files]
  (update-files-handler app #(add-files/add-file-bundle % bundle public-dir files)))

(defn wrap-with-file-bundles [app public-dir bundles]
  (update-files-handler app #(add-files/add-file-bundles % public-dir bundles)))

(defn wrap-with-files [app public-dir files]
  (update-files-handler app #(add-files/add-files % public-dir files)))

(defn wrap-with-referenced-files [app public-dir]
  (update-files-handler app #(add-files/add-referenced-files % public-dir)))

(defn wrap-with-cache-busted-expires-headers [app]
  (update-files-handler app add-cache-busted-expires-headers))

(defn wrap-to-concatenate-bundles [app]
  (update-files-handler app concatenate-bundles))
