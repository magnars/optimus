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
   [{:path "/more.js" :contents "3 + 4" :headers {"Cache-Control" "max-age=315360000"}}])

 (let [app (serve-live-assets noop get-assets dont-optimize {})]
   (app {:uri "/more.js"}) => {:status 200 :body "3 + 4" :headers {"Cache-Control" "max-age=315360000"}}))

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

(fact
 "When loading the assets, the strategy guards against duplicate
  assets by their path. Equal assets are simply collapsed to one."

 (defn get-assets [] [{:path "/code.js" :contents "abc"}
                      {:path "/code.js" :contents "abc"}])

 (let [app (serve-live-assets return-request get-assets dont-optimize {})]
   (app {}) => {:optimus-assets [{:path "/code.js" :contents "abc"}]})

 (let [app (serve-frozen-assets return-request get-assets dont-optimize {})]
   (app {}) => {:optimus-assets [{:path "/code.js" :contents "abc"}]}))

(fact
 "Assets are allowed to be duplicated if they belong to different bundles."

 (defn get-assets [] [{:path "/code.js" :contents "abc" :bundle "app1.js"}
                      {:path "/code.js" :contents "abc" :bundle "app2.js"}])

 (let [app (serve-live-assets return-request get-assets dont-optimize {})]
   (app {}) => {:optimus-assets [{:path "/code.js" :contents "abc" :bundle "app1.js"}
                                 {:path "/code.js" :contents "abc" :bundle "app2.js"}]}))

(fact
 "Duplicate assets that are not equal are not tolerated."

 (defn get-assets [] [{:path "/code.js" :contents "abc"}
                      {:path "/code.js" :contents "def"}])

 (serve-frozen-assets return-request get-assets dont-optimize {})
 => (throws Exception "Two assets have the same path \"/code.js\", but are not equal."))

(fact
 "We can't compare get-stream functions, so those will have to pass."

 (defn get-assets [] [{:path "/code.js" :get-stream (fn [] "a stream")}
                      {:path "/code.js" :get-stream (fn [] "a stream")}])

 (let [app (serve-live-assets return-request get-assets dont-optimize {})]
   (-> (app {}) :optimus-assets count) => 1))

(fact
 "Asset order is preserved."

 (defn get-assets [] [{:path "/a.js" :contents ""}
                      {:path "/b.js" :contents ""}
                      {:path "/c.js" :contents ""}
                      {:path "/d.js" :contents ""}
                      {:path "/e.js" :contents ""}
                      {:path "/f.js" :contents ""}
                      {:path "/g.js" :contents ""}
                      {:path "/h.js" :contents ""}
                      {:path "/i.js" :contents ""}])

 (let [app (serve-live-assets return-request get-assets dont-optimize {})]
   (app {}) => {:optimus-assets [{:path "/a.js" :contents ""}
                                 {:path "/b.js" :contents ""}
                                 {:path "/c.js" :contents ""}
                                 {:path "/d.js" :contents ""}
                                 {:path "/e.js" :contents ""}
                                 {:path "/f.js" :contents ""}
                                 {:path "/g.js" :contents ""}
                                 {:path "/h.js" :contents ""}
                                 {:path "/i.js" :contents ""}]}))
