(ns optimus.angular
  (:require [clojure.string :as str]))

(defn- escaped-js-string
  [s]
  (-> s
      (str/replace "\\" "\\\\")
      (str/replace "\"" "\\\"")
      (str/replace "\n" "\\n")))

(defn- template-cache-put
  [template]
  (let [escaped-contents (escaped-js-string (:contents template))]
    (str "  $templateCache.put(\"" (:path template) "\", \"" escaped-contents "\");\n")))

(defn- create-template-cache-js
  [module templates]
  (str "angular.module(\"" module  "\").run([\"$templateCache\", function ($templateCache) {\n"
       (apply str (map template-cache-put templates))
       "}]);" ))

(defn create-template-cache
  [& {:keys [path module templates bundle]}]
  {:path path
   :bundle bundle
   :contents (create-template-cache-js module templates)})
