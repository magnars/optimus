(ns optimus.optimizations-test
  (:require [optimus.optimizations :as optimizations]
            [clj-time.core :as time])
  (:use midje.sweet))

(with-redefs [time/now (fn [] (time/date-time 2013 07 30))]
  (def headers {"Cache-Control" "max-age=315360000"
                "Expires" "Fri, 28 Jul 2023 00:00:00 GMT"})

  (fact
   "This is a big integration test, for sure. It bundles bundles, it
    adds cache busters and headers. It minifies."

   (optimizations/all
    [{:path "/core.js" :contents "var x = 1 + 2;" :bundle "app.js"}
     {:path "/main.js" :contents "var y = 3 + 4;" :bundle "app.js"}]
    {})

   => [{:path "/core.js" :contents "var x=3;" :outdated true}
       {:path "/main.js" :contents "var y=7;" :outdated true}
       {:path "/bundles/app.js" :contents "var x=3;\nvar y=7;" :bundle "app.js" :outdated true}
       {:path "/1e10b6b7ffe7/core.js" :original-path "/core.js" :contents "var x=3;" :headers headers}
       {:path "/3984012ce8f1/main.js" :original-path "/main.js" :contents "var y=7;" :headers headers}
       {:path "/acc6196d6f45/bundles/app.js" :original-path "/bundles/app.js" :contents "var x=3;\nvar y=7;" :bundle "app.js" :headers headers}]))
