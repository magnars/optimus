(ns optimus.paths
  (:require [pathetic.core :as pathetic]))

(defn just-the-path [path]
  (-> path
      pathetic/parse-path
      pathetic/up-dir
      pathetic/render-path
      pathetic/ensure-trailing-separator))

(defn just-the-filename [path]
  (last (pathetic/split path)))

(defn to-absolute-url [container-url relative-url]
  (-> container-url
      (just-the-path)
      (pathetic/resolve relative-url)
      (pathetic/normalize)))

(defn filename-ext [filename]
  (second (re-find #"\.([^./\\]+)$" filename)))

