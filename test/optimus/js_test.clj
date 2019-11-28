(ns optimus.js-test
  (:use [midje.sweet]
        [optimus.js :as js]))

(fact
  "`preference-str->list` tokenizes comma-separated names"
  (js/preference-str->list "") => []
  (js/preference-str->list ",") => []
  (js/preference-str->list "foo") => ["foo"]
  (js/preference-str->list "foo,") => ["foo"]
  (js/preference-str->list "foo,bar") => ["foo" "bar"]
  (js/preference-str->list "foo bar, baz") => ["foo bar" "baz"]
  (js/preference-str->list ", foo , bar , ") => ["foo" "bar"]
  (js/preference-str->list "foo-bar
                           ,
                           baz") => ["foo-bar" "baz"])


(defn fake-script-engine
  [^String name]
  (proxy [javax.script.ScriptEngine] []
    (toString [] name)))

(def fake-manager
  (proxy [javax.script.ScriptEngineManager] []
    (getEngineByName [^String name]
      (get {"foo" (fake-script-engine "foo-engine")
            "bar" (fake-script-engine "bar-engine")
            "baz" (fake-script-engine "baz-engine")}
           name))))

(fact
  "`first-available-engine` finds the first available engine for a script manager"
  (str (js/first-available-engine fake-manager ["foo" "bar"])) => "foo-engine"
  (str (js/first-available-engine fake-manager ["bar" "foo"])) => "bar-engine"
  (str (js/first-available-engine fake-manager ["baz"])) => "baz-engine"
  (js/first-available-engine fake-manager ["qux"]) => nil
  (js/first-available-engine fake-manager []) => nil
  (str (js/first-available-engine fake-manager ["foo" :oops-not-reached])) => "foo-engine"
  (js/first-available-engine fake-manager ["skipme" :oops]) => (throws Exception))


(fact
  "`make-engine` returns an available engine or throws"
  (instance? javax.script.ScriptEngine (make-engine {:prefer "graal.js"})) => true
  (instance? javax.script.ScriptEngine (make-engine {:prefer ["graal.js"]})) => true
  (instance? javax.script.ScriptEngine (make-engine {:prefer ["skipme" "graal.js"]})) => true
  (instance? javax.script.ScriptEngine (make-engine {:prefer "skipme, graal.js"})) => true
  (instance? javax.script.ScriptEngine (make-engine {:prefer "skipme"})) => (throws clojure.lang.ExceptionInfo)
  (instance? javax.script.ScriptEngine (make-engine {:prefer ""})) => (throws clojure.lang.ExceptionInfo)
  (instance? javax.script.ScriptEngine (make-engine {:prefer []})) => (throws clojure.lang.ExceptionInfo))

(fact
  "`with-engine` evaluates body and closes AutoCloseable engines such as GraalJS"
  (let [eng (make-engine {:prefer "graal.js"})]
    (js/with-engine [e eng] (.eval e "5 + 5"))) => 10
  (let [eng (make-engine {:prefer "graal.js"})]
    (js/with-engine [e eng] (.eval e "\"still open\""))
    (.eval eng "\"closed now!\"")) => (throws IllegalStateException))

(fact
  "`run-script-with-error-handling` runs the script and converts results to Clojure values"
  (js/with-engine [eng (js/make-engine {:prefer "graal.js"})]
    (js/run-script-with-error-handling
      eng "32 + 32" "<none>")) => 64
  (js/with-engine [eng (js/make-engine {:prefer "graal.js"})]
    (js/run-script-with-error-handling
      eng "Object" "<none>")) => {})

(fact
  "`run-script-with-error-handling` throws a script-error or engine-error when there is a problem"
  (js/with-engine [eng (js/make-engine {:prefer "graal.js"})]
    (try (js/run-script-with-error-handling
           eng "Object.nonexisting()" "<none>")
         (catch clojure.lang.ExceptionInfo e (ex-data e)))) => (contains {:type :optimus.js/script-error})
  (js/with-engine [eng (js/make-engine {:prefer "graal.js"})]
    (try (js/run-script-with-error-handling
           eng "syntax_error =" "<none>")
         (catch clojure.lang.ExceptionInfo e (ex-data e)))) => (contains {:type :optimus.js/engine-error}))
