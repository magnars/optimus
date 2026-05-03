(ns optimus.javascript
  (:require [clojure.data.json :as json])
  (:import [org.graalvm.polyglot Context PolyglotException]))

(defn load-scripts [context scripts]
  (doseq [script scripts]
    (.eval context "js" script)))

(defn create-context [& [{:keys [scripts]}]]
  (let [prop (System/getProperty "polyglot.engine.WarnInterpreterOnly")]
    (try
      (System/setProperty "polyglot.engine.WarnInterpreterOnly" "false")
      (doto (Context/create (into-array String ["js"]))
        (.eval "js" "globalThis.window = { XMLHttpRequest: {} };")
        (load-scripts scripts))
      (finally
        (when prop
          (System/setProperty "polyglot.engine.WarnInterpreterOnly" prop))))))

(defn ^{:indent 1} clj->js [^Context context m]
  (.eval context "js" (str "(" (json/write-str m) ")")))

(defn call-method [object method & args]
  (-> (.getMember object (name method))
      (.execute (into-array Object args))))

(defn get-string [object prop]
  (.asString (.getMember object (name prop))))

(defn call-global-fn [^Context context fn & args]
  (-> (.getBindings context "js")
      (.getMember (name fn))
      (.execute (into-array Object args))))

(defn construct [context constructor options]
  (.newInstance constructor (into-array Object [(clj->js context options)])))

(defn wrap-js-error [^PolyglotException e]
  (if (.isGuestException e)
    (let [loc (.getSourceLocation e)]
      (ex-info (.getMessage e)
               (cond-> {:type ::script-error
                        :engine "graaljs"}
                 loc (assoc :line (.getStartLine loc)
                            :col (.getStartColumn loc)
                            :source (some-> loc .getSource .getName)))
               e))
    (ex-info (.getMessage e)
             {:type ::engine-error
              :engine "graaljs"}
             e)))

(defmacro with-error-handling [& body]
  `(try
     ~@body
     (catch PolyglotException e#
       (throw (wrap-js-error e#)))))
