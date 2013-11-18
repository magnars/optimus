(ns optimus.strategies-test
  (:use [optimus.strategies]
        [midje.sweet]))

(defn noop [_])
(defn return-request [req] req)

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

 (let [app (serve-unchanged-assets noop #(deref assets))]
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
