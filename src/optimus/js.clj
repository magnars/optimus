(ns optimus.js
  (:require
    [clojure.string :as str]
    [environ.core :refer [env]]))

(def default-engines ["graal.js" "rhino" "nashorn"])

(defn preference-str->list
  [comma-separated-engine-names]
  (-> comma-separated-engine-names
      str/trim
      (str/split #"\s*,\s*")
      (->> (map str/trim)
           (remove empty?))))

(defn first-available-engine
  [manager engine-names]
  (when-not (empty? engine-names)
    (loop [engine-name (first engine-names)
           remaining   (rest engine-names)]
      (if-let [eng (.getEngineByName manager engine-name)]
        eng
        (when-not (empty? remaining)
          (recur (first remaining) (rest remaining)))))))

(defn make-js-engine
  ([preference-list-or-str]
   ;;{:pre [(truss/have? #(or (sequential? %) (string? %)) preference-list-or-str)]}
   (let [preference-list (->> (if (string? preference-list-or-str)
                                (preference-str->list preference-list-or-str)
                                preference-list-or-str))
         manager         (javax.script.ScriptEngineManager.)
         engine          (first-available-engine manager preference-list)]
     (or engine
         (throw (ex-info (str "No preferred JS engines found named "
                              (pr-str preference-list)
                              " among available engines "
                              (.getEngineFactories manager))
                         {:error           :preferred-js-engines-not-found
                          :preference-list preference-list
                          :factories       (.getEngineFactories manager)})))))
  ([]
   (make-js-engine
     (or (env :optimus-js-engines)
         default-engines))))
