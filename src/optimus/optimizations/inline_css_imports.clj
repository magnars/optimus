(ns optimus.optimizations.inline-css-imports
  (:require [clojure.string :as str]
            [optimus.assets.load-css :refer [update-css-references external-url?]]
            [optimus.homeless :refer [assoc-non-nil max?]]))

(def import-re #"@import (?:url)?[('\"]{1,2}([^']+?)[)'\"]{1,2} ?([^;]*);")

(defn by-path [path assets]
  (first (filter #(= path (:path %)) assets)))

(defn inline-import-match [asset assets [match path media]]
  (if (external-url? path)
    (throw (Exception. (str "Import of external URL " path " in " (:path asset) " is strongly adviced against. It's a performance killer. In fact, there's no option to allow this. Use a link in your HTML instead. Open an issue if you really, really need it.")))
    (let [contents (:contents (by-path path assets))]
      (if (empty? media)
        contents
        (str "@media " media " { " contents " }")))))

(defn is-css [#^String path]
  (.endsWith path ".css"))

(defn referenced-assets [asset assets]
  (map #(by-path % assets) (:references asset)))

(defn inline-css-imports-1 [asset assets]
  (if-not (is-css (:path asset))
    asset
    (-> asset
        (assoc :contents (str/replace (:contents asset) import-re (partial inline-import-match asset assets)))
        (assoc-non-nil :last-modified (max? (keep :last-modified (conj (referenced-assets asset assets) asset))))
        (update-css-references))))

(defn inline-css-imports [assets]
  (map #(inline-css-imports-1 % assets) assets))
