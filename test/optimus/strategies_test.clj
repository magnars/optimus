(ns optimus.strategies-test
  (:use [juxt.dirwatch :only [watch-dir]]
        [midje.sweet]
        [optimus.homeless]
        [optimus.strategies]
        [test-with-files.core]))

(defn noop [_])
(defn return-request [req] req)

(defn dont-optimize [assets options] assets)

(fact
 "serve-live-assets serves the assets :contents when the request :uri
  matches the :path."

 (defn get-assets []
   [{:path "/code.js" :contents "1 + 2"}])

 (let [handler (fn [req] {:status 404 :body "Not found"})
       app (serve-live-assets handler get-assets dont-optimize {})]
   (app {:uri "/code.js"}) => {:status 200 :body "1 + 2"}
   (app {:uri "/gone.js"}) => {:status 404 :body "Not found"})

 (let [handler (fn [req respond raise] (respond {:status 404 :body "Not found"}))
       app (serve-live-assets handler get-assets dont-optimize {})
       response (atom nil)
       respond (partial reset! response)]
   (app {:uri "/code.js"} respond noop)
   @response => {:status 200 :body "1 + 2"}
   (app {:uri "/gone.js"} respond noop)
   @response => {:status 404 :body "Not found"}))

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

(fact
 "serve-live-assets-autorefresh caches assets and refreshes the cache when files are changed"

 (let [file-path "/code.js"
       full-file-path (str tmp-dir file-path)
       watchdir-callback (atom nil)
       watchdir-path (atom nil)]

   (defn get-assets []
     [{:path file-path :contents (slurp full-file-path)}])

   (defn notify-about-file-change []
     (@watchdir-callback {:file (clojure.java.io/file full-file-path)}))

   (with-files [[file-path "abc"]]
     (with-redefs [watch-dir (fn [callback path]
                               (reset! watchdir-callback callback)
                               (reset! watchdir-path path))]

       (let [handler (fn [req] {:status 404 :body "Nothing"})
             app (serve-live-assets-autorefresh handler get-assets dont-optimize {:assets-dir tmp-dir})]
         @watchdir-path => (clojure.java.io/file tmp-dir) ;; we are watching a directory passed via options
         (app {:uri "/code.js"}) => {:status 200 :body "abc"}
         (spit full-file-path "def")
         (app {:uri "/code.js"}) => {:status 200 :body "abc"} ;; still cached
         (notify-about-file-change)
         (app {:uri "/code.js"}) => {:status 200 :body "def"}

         (fact
          "passes unknown request to app"
          (app {:uri "/nope"}) => {:status 404 :body "Nothing"}))

       (fact
        "resources directory is watched when :assets-dir option is not specified"
        (let [app (serve-live-assets-autorefresh noop get-assets dont-optimize {})]
          @watchdir-path => (clojure.java.io/file "resources")))))

   (with-files [[file-path "abc"]]
     (with-redefs [watch-dir (fn [callback path]
                               (reset! watchdir-callback callback)
                               (reset! watchdir-path path))]

       (let [handler (fn [req respond raise] (respond {:status 404 :body "Nothing"}))
             app (serve-live-assets-autorefresh handler get-assets dont-optimize {:assets-dir tmp-dir})
             response (atom nil)
             respond (partial reset! response)]
         @watchdir-path => (clojure.java.io/file tmp-dir) ;; we are watching a directory passed via options
         (app {:uri "/code.js"} respond noop)
         @response => {:status 200 :body "abc"}
         (spit full-file-path "def")
         (app {:uri "/code.js"} respond noop)
         @response => {:status 200 :body "abc"} ;; still cached
         (notify-about-file-change)
         (app {:uri "/code.js"} respond noop)
         @response => {:status 200 :body "def"}

         (fact
          "async passes unknown request to app"
          (app {:uri "/nope"} respond noop)
          @response => {:status 404 :body "Nothing"}))))))

(fact
 "serve-live-assets serves the assets :contents when the request :uri
  matches the :context-path and :path."

 (defn get-assets []
   [{:path "/code.js" :context-path "/somewhere" :contents "1 + 2"}])

 (let [app (serve-live-assets noop get-assets dont-optimize {})]
   (app {:uri "/code.js"}) => nil
   (app {:uri "/somewhere/code.js"}) => {:status 200 :body "1 + 2"}))

(fact
 "serve-live-assets-autorefresh serves the assets :contents when the
  request :uri matches the :context-path and :path."

 (defn get-assets []
   [{:path "/code.js" :context-path "/somewhere" :contents "1 + 2"}])

 (let [app (serve-live-assets-autorefresh noop get-assets dont-optimize {})]
   (app {:uri "/code.js"}) => nil
   (app {:uri "/somewhere/code.js"}) => {:status 200 :body "1 + 2"}))

(fact
 "serve-frozen-assets serves the assets :contents when the request :uri
  matches the :context-path and :path."

 (defn get-assets []
   [{:path "/code.js" :context-path "/somewhere" :contents "1 + 2"}])

 (let [app (serve-frozen-assets noop get-assets dont-optimize {})]
   (app {:uri "/code.js"}) => nil
   (app {:uri "/somewhere/code.js"}) => {:status 200 :body "1 + 2"})

 (let [handler (fn [req] {:status 404 :body "Nope"})
       app (serve-frozen-assets handler get-assets dont-optimize {})]
   (app {:uri "/code.js"}) => {:status 404 :body "Nope"}
   (app {:uri "/somewhere/code.js"}) => {:status 200 :body "1 + 2"}
   (app {:uri "/gone"}) => {:status 404 :body "Nope"})

 (let [response (atom nil)
       respond (partial reset! response)
       handler (fn [req respond raise] (respond {:status 404 :body "Nope"}))
       app (serve-frozen-assets handler get-assets dont-optimize {})]
   (app {:uri "/somewhere/code.js"} respond noop)
   @response => {:status 200 :body "1 + 2"}
   (app {:uri "/"} respond noop)
   @response => {:status 404 :body "Nope"}))
