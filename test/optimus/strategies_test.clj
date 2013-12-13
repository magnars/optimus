(ns optimus.strategies-test
  (:use [optimus.strategies]
        [midje.sweet]))

(defn noop [_])
(defn return-request [req] req)

(defn dont-optimize [assets options] assets)

(fact
 "serve-live-assets serves the assets :contents when the request :uri
  matches the :path."

 (defn get-assets []
   [{:path "/code.js" :contents "1 + 2"}])

 (let [app (serve-live-assets noop get-assets dont-optimize {})]
   (app {:uri "/code.js"}) => {:status 200 :body "1 + 2"}
   (app {:uri "/gone.js"}) => nil))

(fact
 "Headers are included."

 (defn get-assets []
   [{:path "/more.js" :contents "3 + 4" :headers {"Expires" "Fri, 28 Jul 2023 00:00:00 GMT"}}])

 (let [app (serve-live-assets noop get-assets dont-optimize {})]
   (app {:uri "/more.js"}) => {:status 200 :body "3 + 4" :headers {"Expires" "Fri, 28 Jul 2023 00:00:00 GMT"}}))

(fact
 "Assets are fetched for each request. We wouldn't want to restart the
  server for every file change."

 (def assets (atom []))

 (let [app (serve-live-assets noop #(deref assets) dont-optimize {:cache-live-assets false})]
   (app {:uri "/code.js"}) => nil
   (swap! assets conj {:path "/code.js" :contents "1 + 2"})
   (app {:uri "/code.js"}) => {:status 200 :body "1 + 2"}))

(fact
 "If the :uri does not match a :path, the assets are added to the
  request to be used by linking functions."

 (defn get-assets []
   [{:path "/code.js" :contents "1 + 2"}])

 (let [app (serve-live-assets return-request get-assets dont-optimize {})]
   (app {:uri "/index.html"}) => {:uri "/index.html"
                                  :optimus-assets [{:path "/code.js" :contents "1 + 2"}]}))

(fact
 "We're serving frozen assets in production, both for performance
  reasons, but also to ensure we're not serving different contents at
  the same url."

 (def assets (atom [{:path "/core.js" :contents "var x = 1 + 2;"}]))

 (let [app (serve-frozen-assets noop #(deref assets) dont-optimize {})]
   (app {:uri "/core.js"}) => {:status 200 :body "var x = 1 + 2;"}
   (swap! assets conj {:path "/core.js" :contents "var y = 3 + 4"})
   (app {:uri "/core.js"}) => {:status 200 :body "var x = 1 + 2;"}))

(fact
 "The assets are transformed by the optimize function passed to the strategy."

 (defn optimize [assets options]
   (map #(assoc % :size 1) assets))

 (defn get-assets [] [{:path "/code.js" :size 10}])

 (let [app (serve-live-assets return-request get-assets optimize {})]
   (app {}) => {:optimus-assets [{:path "/code.js" :size 1}]})

 (let [app (serve-frozen-assets return-request get-assets optimize {})]
   (app {}) => {:optimus-assets [{:path "/code.js" :size 1}]}))
