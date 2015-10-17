(ns optimus.js
  "Wraps javax.scripting to provide a JSR-223 compliant ECMAScript engine. Engine
   selection is influenced by environ, if no programmatic choice is supplied."
  (:require [environ.core :refer [env]])
  (:import javax.script.ScriptEngineManager))

(def default-engine-name "clj-v8")

(def engines (ScriptEngineManager.))

(defmulti cleanup-engine
  "Invoke a cleanup procedure if the implementing ScriptEngine instance is
   known to support it."
  #(-> % .getFactory .getNames first))

(defmethod cleanup-engine "clj-v8"
  [engine]
  ;; Explicitly clean up V8 ScriptEngine instances, if we don't want to wait for GC to get around to finalize().
  (.cleanup engine))

(defmethod cleanup-engine :default
  [engine]
  ;; Do nothing as Nashorn ScriptEngine or other instances should be cleaned up by GC.
  nil)


(defn get-engine
  "Return an instance of a javax.script.ScriptEngine implementation class,
   selected according to engine-name, or environ's optimus-js-engine variable,
   or the built-in default engine, respectively.

   Note: if engine-name is supplied but not found by javax.script.ScriptEngineManager,
   an exception is raised."
  ([engine-name]
     (or (.getEngineByName engines engine-name)
         (throw (Exception. (str "JS script engine " engine-name " could not be loaded.")))))
  ([]
     (get-engine (or (env :optimus-js-engine)
                     default-engine-name))))
