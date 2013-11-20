(ns optimus.strategies-test
  (:use [optimus.strategies]
        [midje.sweet])
  (:require [clj-time.core :as time]))

(defn noop [_])
(defn return-request [req] req)

;; serve-unchanged-assets

(fact
 "Serving unchanged assets means they're not touched, yo."

 (defn get-assets []
   [{:path "/code.js" :contents "1 + 2"}])

 (let [app (serve-unchanged-assets noop get-assets)]
   (app {:uri "/code.js"}) => {:status 200 :body "1 + 2"}
   (app {:uri "/gone.js"}) => nil))

(fact
 "Headers are included."

 (defn get-assets []
   [{:path "/more.js" :contents "3 + 4" :headers {"Cache-Control" "max-age=315360000"}}])

 (let [app (serve-unchanged-assets noop get-assets)]
   (app {:uri "/more.js"}) => {:status 200 :body "3 + 4" :headers {"Cache-Control" "max-age=315360000"}}))

(fact
 "Assets are fetched for each request. We wouldn't want to restart the
  server for every file change."

 (def assets (atom []))

 (let [app (serve-unchanged-assets noop #(deref assets) {:cache-live-assets false})]
   (app {:uri "/code.js"}) => nil
   (swap! assets conj {:path "/code.js" :contents "1 + 2"})
   (app {:uri "/code.js"}) => {:status 200 :body "1 + 2"}))

(fact
 "If there is no matching path, add the files to the request to be
  used by linking functions."

 (defn get-assets []
   [{:path "/code.js" :contents "1 + 2"}])

 (let [app (serve-unchanged-assets return-request get-assets)]
   (app {:uri "/index.html"}) => {:uri "/index.html"
                                  :optimus-assets [{:path "/code.js" :contents "1 + 2"}]}))

;; optimized

(with-redefs [time/now (fn [] (time/date-time 2013 07 30))]
  (def headers {"Cache-Control" "max-age=315360000"
                "Expires" "Fri, 28 Jul 2023 00:00:00 GMT"})

  ;; serve-optimized-assets

  (fact
   "This is a big integration test, for sure. It bundles bundles, it
    adds cache busters and headers."

   (defn get-assets []
     [{:path "/core.js" :contents "var x = 1 + 2;" :bundle "app.js"}
      {:path "/main.js" :contents "var y = 3 + 4;" :bundle "app.js"}])

   (let [app (serve-optimized-assets return-request get-assets)]
     (:optimus-assets (app {})) =>

     [{:path "/core.js" :contents "var x = 1 + 2;" :outdated true}
      {:path "/main.js" :contents "var y = 3 + 4;" :outdated true}
      {:path "/bundles/app.js" :contents "var x = 1 + 2;\nvar y = 3 + 4;" :bundle "app.js" :outdated true}
      {:path "/7c88f58018c5/core.js" :original-path "/core.js" :contents "var x = 1 + 2;" :headers headers}
      {:path "/56ea15143f13/main.js" :original-path "/main.js" :contents "var y = 3 + 4;" :headers headers}
      {:path "/f37403af7031/bundles/app.js" :original-path "/bundles/app.js" :contents "var x = 1 + 2;\nvar y = 3 + 4;" :bundle "app.js" :headers headers}]))

  (fact
   "The serve-optimized-assets strategy is useful to debug in the same
    environment as production, since everything is minified and
    optimized, but it is still live - no need to restart for every
    change."

   (def assets (atom [{:path "/core.js" :contents "var x = 1 + 2;"}]))

   (let [app (serve-optimized-assets noop #(deref assets) {:cache-live-assets false})]
     (app {:uri "/core.js"}) => {:status 200 :body "var x = 1 + 2;"}
     (swap! assets conj {:path "/core.js" :contents "var y = 3 + 4;"})
     (app {:uri "/core.js"}) => {:status 200 :body "var y = 3 + 4;"}))

  ;; serve-frozen-optimized-assets

  (fact
   "We're serving frozen assets in production, both for performance
    reasons, but also to ensure we're not serving different contents at
    the same url."

   (def assets (atom [{:path "/core.js" :contents "var x = 1 + 2;"}]))

   (let [app (serve-frozen-optimized-assets noop #(deref assets))]
     (app {:uri "/7c88f58018c5/core.js"}) => {:status 200 :body "var x = 1 + 2;" :headers headers}
     (swap! assets conj {:path "/core.js" :contents "var y = 3 + 4"})
     (app {:uri "/7c88f58018c5/core.js"}) => {:status 200 :body "var x = 1 + 2;" :headers headers})))
