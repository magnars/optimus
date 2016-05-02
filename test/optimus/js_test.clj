(ns optimus.js-test
  (:require [optimus.js :refer :all]
            [midje.sweet :refer [fact => throws anything]]))

(fact
 "Gets a default JSR-223 script engine (potentially influenced by environ)"
 (get-engine) instance? javax.script.ScriptEngine)

(fact
 "Gets a V8 script engine when requested"
 (type (get-engine "clj-v8")) => clj_jsr223_v8.V8ScriptEngine)

(fact
 "Throws an exception when asked for an invalid script engine"
 (get-engine "/invalid/") => (throws Exception "No JS script engine could be loaded from: /invalid/"))

(fact
 "Throws an exception when asked for multiple invalid script engines"
 (get-engine "/invalid1/,/invalid2/") => (throws Exception "No JS script engine could be loaded from: /invalid1/, /invalid2/"))

(fact
 "Can manually cleanup a V8 script engine"
 (cleanup-engine (get-engine "clj-v8")) => anything)

(fact
 "Can evaluate trivial JS code in clj-v8 script engine"
 (let [engine (get-engine "clj-v8")]
   (.eval engine "var a = 1")
   (.eval engine "a")) => 1)

(fact
 "Throws an exception when trying to reuse a cleaned-up V8 script engine"
 (let [engine (get-engine "clj-v8")]
   (cleanup-engine engine)
   (.eval engine "true"))  => (throws NullPointerException))

(fact
 "Can manually cleanup the default engine (potentially influenced by environ)"
 (cleanup-engine (get-engine)) => anything)

(fact
 "Can evaluate trivial JS code in clj-v8 script engine (potentially influenced by environ)"
 (let [engine (get-engine)]
   (.eval engine "var a = 1")
   (.eval engine "a")) => 1)

(fact
 "Reads a string list of engine names correctly"
 (read-engine-names-str "") => []
 (read-engine-names-str "clj-v8") => ["clj-v8"]
 (read-engine-names-str "clj-v8,, ,") => ["clj-v8"]
 (read-engine-names-str "nashorn,clj-v8") => ["nashorn", "clj-v8"]
 (read-engine-names-str "nashorn, clj-v8") => ["nashorn", "clj-v8"])
