(ns optimus.js
  "Wraps javax.scripting to provide a JSR-223 compliant ECMAScript engine. Engine
   selection is influenced by environ, if no programmatic choice is supplied."
  (:require [environ.core :refer [env]])
  (:import javax.script.ScriptEngineManager))

(def default-engine-name "clj-v8")

(defn read-engine-names-str
  [names-str]
  (->> (clojure.string/split names-str #",")
       (map clojure.string/trim)
       (filter (complement empty?))))

(def engines (ScriptEngineManager.))

(defmulti cleanup-engine
  "Invoke a cleanup procedure if the implementing ScriptEngine instance is
   known to support it."
  #(-> % .getFactory .getNames first))

(defmethod cleanup-engine "clj-v8"
  [engine]
  ;; Explicitly clean up V8 ScriptEngine instances, if we don't want to wait for GC to get around to finalize().
  (.cleanup engine)
  engine)

(defmethod cleanup-engine :default
  [engine]
  ;; Do nothing as Nashorn ScriptEngine or other instances should be cleaned up by GC.
  engine)


(defmulti init-engine
  "Initialize the bare engine with any implementation-specific patches etc."
  #(-> % .getFactory .getNames first))

(defmethod init-engine "nashorn"
  [engine]
  (.eval engine "
    (function(){
      var nashorn_splice = Array.prototype.splice;
      Array.prototype.splice = function() {
      if (arguments.length === 1) {
       return nashorn_splice.call(this, arguments[0], this.length);
      } else {
       return nashorn_splice.apply(this, arguments);
     }}})();")
  engine)

(defmethod init-engine :default
  [engine]
  engine)

(defn- get-engine-by-name-safely
  [engines name]
  (try
    (.getEngineByName engines name)
    (catch Exception e nil)))

(defn get-engine
  "Return an instance of a javax.script.ScriptEngine implementation class,
   selected according to names-str, or environ's optimus-js-engine variable,
   or the built-in default engine, respectively.

   names-str must be a comma-separated string of one or more engine names
   that javax.script.ScriptEngineManager may recognise, in decreasing order
   of preference from left to right.

   Note: if names-str is supplied but no matching engine is found by
   javax.script.ScriptEngineManager, an exception is raised."
  ([names-str]
     (let [engine-names (read-engine-names-str names-str)]
       (if (empty? engine-names)
         (throw (Exception. "No engine name(s) given."))
         (if-let [engine (->> engine-names
                              (map (partial get-engine-by-name-safely engines))
                              (filter (complement nil?))
                              first)]
           (init-engine engine)
           (throw (Exception. (apply str "No JS script engine could be loaded from: "
                                     (interpose ", " engine-names))))))))
  ([]
     (get-engine (or (env :optimus-js-engine)
                     default-engine-name))))
