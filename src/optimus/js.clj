(ns optimus.js
  (:require
    [clojure.string :as str]
    [clojure.java.data :as java.data]
    [environ.core :refer [env]]))

(def default-engines
  "Officially supported JS engines (which may or may not be available
  in a given runtime), in descending order of default preference. The
  first available engine from this list will be instantiated when
  `make-engine` is invoked without a configured preference."
  ["graal.js" "rhino" "nashorn"])

(defn preference-str->list
  "Tokenizes a comma-separated string of engine names into a Clojure
  sequence of engine-name strings. Whitespace gets trimmed around each
  engine name and empty names get removed.

  Valid inputs may look like:

      \"foo,baz\" => (\"foo\", \"baz\")
         \"foo\"  => (\"foo\")
    \"  bar  ,,\" => (\"bar\")"
  [comma-separated-engine-names]
  (-> comma-separated-engine-names
      str/trim
      (str/split #"\s*,\s*")
      (->> (map str/trim)
           (remove empty?))))

(defn first-available-engine
  "Returns a newly instantiated javax.script.ScriptEngine for the
  first available engine name discoverable by the `manager`
  javax.script.ScriptEngineManager, in the given sequence `engine-names`,
  in left-to-right order of precedence. If multiple engines are available
  to the manager, only the first one gets instantiated. If no engines are
  available, `nil` is returned."
  [manager engine-names]
  (when-not (empty? engine-names)
    (loop [engine-name (first engine-names)
           remaining   (rest engine-names)]
      (if-let [eng (.getEngineByName manager engine-name)]
        eng
        (when-not (empty? remaining)
          (recur (first remaining) (rest remaining)))))))

(defn make-engine
  "Returns a newly instantiated javax.script.ScriptEngine for the
  first available engine name listed in the `preferences-list-or-str`,
  which can be a comma-separated string of engine names, or a sequence
  of engine-name strings. If no engine preference argument is given,
  the default value is read from `:optimus-js-engines` using `environ`,
  which may be a key defined by the build tool, a Java property
  (`java -Doptimus.js.engines=...`), or a system environment
  variable (`OPTIMUS_JS_ENGINES=...`). If no environ value is given,
  `default-engines` is used. If no available engine is found for
  the given engine names, an exception is thrown."
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
   (make-engine
     (or (env :optimus-js-engines)
         default-engines))))

(defmacro with-engine
  "Evaluates body in a try expression with `lname` bound to the value
  `engine` which should be a javax.script.ScriptEngine instance. The
  engine instance may implement java.lang.AutoCloseable (such as with
  GraalJS), in which case the finally clause of the try expression
  will attempt to close it. Note: not all JS engines implement
  AutoCloseable so explicit cleanup is not guaranteed for every engine."
  [[lname engine] & body]
  `(let [~lname ~engine]
     (try
       ~@body
       (finally
         (when (instance? java.lang.AutoCloseable ~lname)
           (try
             (.close ~lname)
             (finally nil)))))))

(def error-preamble-code
  "var __OPTIMUS_JS_ERROR = null;
   function OptimusJSError(msg, line, col) {
     __OPTIMUS_JS_ERROR = {\"message\": msg, \"line\": line || -1, \"col\": col || -1};
   }
   var console = {
     error: function (message) {
         throw new OptimusJSError(message);
     }
   };")

(defn wrap-with-error
  "Wraps the given JS script string in a try/catch block that re-throws
  an instance of `OptimusJSError` (which sets up a magic variable containing
  error message/line/col JS Error information. JS Error info is not otherwise
  consistently available to the engine host Exception hierarchy, so this in-band
  communication channel is useful when used with `run-script-with-error-handling`."
  [script]
  (format "%s%n try { %s%n } catch (e) { throw new OptimusJSError(e.message, e.line, e.col); };"
          error-preamble-code script))

(defn optimus-js-error
  "Returns the last Optimus JS error in `engine` as an instance of `ExceptionInfo`
  with line/col/path/engine properties with type `::script-error`, cause `error`
  and a uniform message string (across all supported JS engines). If no Optimus JS
  error is found, it is assumed that some unknown exception `error` has occurred,
  so an `ExceptionInfo` of type `::engine-error` is returned. The returned values
  are intended to be re-thrown by the caller after catching a script exception
  `error` (that may be lacking useful info or varies across engines)."
  [engine file-path error]
  (if-let [err (try (java.data/from-java (.eval engine "__OPTIMUS_JS_ERROR;"))
                    (catch Exception e nil))]
    (let [line (int (get err "line"))
          col  (int (get err "col"))
          msg  (get err "message")]
      (ex-info (format "%s%s%s"
                       (if file-path (format "Exception in %s: " file-path) "")
                       msg
                       (if (and line col) (format " (line %s, col %s)" line col) ""))
               {:line   line
                :col    col
                :path   file-path
                :type   ::script-error
                :engine (-> engine .getFactory .getEngineName)}
               error))
    (ex-info (.getMessage error)
             {:path   file-path
              :type   ::engine-error
              :engine (-> engine .getFactory .getEngineName)}
             error)))

(defn run-script-with-error-handling
  "Returns the result of evaluating the JS `script` string in `engine`. The result
  is converted to a Clojure value using `clojure.java.data/from-java`. Typically
  the JS script should be wrapped in a IIFE block to be useful. If a script
  exception occurs, an `ExceptionInfo` is thrown of either type `::script-error`
  or `::engine-error`."
  [engine script file-path]
  (try (java.data/from-java (.eval engine (wrap-with-error script)))
       (catch Exception e
         ;; TODO verbose logging here would be nice to inspect the wrapped script
         (throw (optimus-js-error engine file-path e)))))
