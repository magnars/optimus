(ns optimus.angular
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- escaped-js-string
  [s]
  (-> s
      (str/replace "\\" "\\\\")
      (str/replace "\"" "\\\"")
      (str/replace "\n" "\\n")))

(defn- template-cache-put
  [public-dir template]
  (let [contents (slurp (io/resource (str public-dir template)))
        escaped-contents (escaped-js-string contents)]
   (str "  $templateCache.put(\"" template "\", \"" escaped-contents "\");\n")))

(defn- create-template-cache-js
  [module public-dir templates]
  (str "angular.module(\"" module  "\").run([\"$templateCache\", function ($templateCache) {\n"
       (apply str (map (partial template-cache-put public-dir) templates))
       "}]);" ))

(defn create-template-cache
  [& {:keys [path module public-dir templates bundle]}]
  {:path path
   :bundle bundle
   :contents (create-template-cache-js module public-dir templates)})
