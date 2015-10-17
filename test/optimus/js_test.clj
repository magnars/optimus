(ns optimus.js-test
  (:require [optimus.js :refer :all]
            [midje.sweet :refer [fact => throws]]))

(fact
 "Gets a default JSR-223 script engine (potentially influenced by environ)"
 (get-engine) instance? javax.script.ScriptEngine)

(fact
 "Gets a V8 script engine when requested"
 (type (get-engine "clj-v8")) => clj_jsr223_v8.V8ScriptEngine)

(fact
 "Throws an exception when asked for an invalid script engine"
 (get-engine "/invalid/") => (throws Exception "JS script engine /invalid/ could not be loaded."))

(fact
 "Can manually cleanup a V8 script engine"
 (cleanup-engine (get-engine "clj-v8")) => nil)

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
 (cleanup-engine (get-engine)) => nil)

(fact
 "Can evaluate trivial JS code in clj-v8 script engine (potentially influenced by environ)"
 (let [engine (get-engine)]
   (.eval engine "var a = 1")
   (.eval engine "a")) => 1)
