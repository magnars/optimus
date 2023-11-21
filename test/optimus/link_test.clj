(ns optimus.link-test
  (:use [midje.sweet]
        [optimus.link]))

(fact
 "You can link to a specific file by its original path. Outdated files
  are skipped."

 (let [request {:optimus-assets [{:path "/bg.png"}
                                 {:path "/main.js" :outdated true}
                                 {:path "/12/m.js" :original-path "/main.js"}]}]

   (file-path request "/bg.png") => "/bg.png"
   (file-path request "/main.js") => "/12/m.js"))

(fact
 "You can optionally add a fallback file."

 (let [request {:optimus-assets [{:path "/bg.png"}
                                 {:path "/known.png"}]}]


   (file-path request "/known.png" :fallback "/bg.png") => "/known.png"
   (file-path request "/unknown.png" :fallback "/bg.png") => "/bg.png"))

(fact
  "You can link to optimized paths."

  (let [request {:optimus-assets [{:path "/main.js" :bundle "app.js" :outdated true}
                                  {:path "/12/m.js" :bundle "app.js"}]}]

    (file-path request "/12/m.js") => "/12/m.js"))

(fact
 "You can link to files specified by their bundle names."

 (let [request {:optimus-assets [{:path "/bg.png"}
                                 {:path "/main.js" :bundle "app.js"}
                                 {:path "/more.js" :bundle "app.js"}
                                 {:path "/other.js" :bundle "lib.js"}]}]

   (bundle-paths request ["app.js"]) => ["/main.js" "/more.js"]
   (bundle-paths request ["app.js" "lib.js"]) => ["/main.js" "/more.js" "/other.js"]))

(fact
 "Outdated files are skipped for bundles too."

 (let [request {:optimus-assets [{:path "/main.js" :bundle "app.js" :outdated true}
                                 {:path "/12/m.js" :bundle "app.js"}]}]

   (bundle-paths request ["app.js"]) => ["/12/m.js"]))

(fact
 "You can link to assets that are by default served on another server,
  by setting the :base-url property."

 (let [request {:optimus-assets [{:path "/main.js"
                                  :base-url "http://cache.example.com"
                                  :bundle "app.js"}]}]

   (file-path request "/main.js") => "http://cache.example.com/main.js"
   (bundle-paths request ["app.js"]) => ["http://cache.example.com/main.js"]))

(fact
 "You can link to assets that are served from a web server directory,
  by setting the :context-path property."

 (let [request {:optimus-assets [{:path "/main.js"
                                  :context-path "/somewhere"
                                  :bundle "app.js"}]}]

   (file-path request "/main.js") => "/somewhere/main.js"
   (bundle-paths request ["app.js"]) => ["/somewhere/main.js"]))

(fact
 "You can combine the :base-url and :context-path properties."

 (let [request {:optimus-assets [{:path "/main.js"
                                  :base-url "http://example.com"
                                  :context-path "/somewhere"
                                  :bundle "app.js"}]}]

   (file-path request "/main.js") => "http://example.com/somewhere/main.js"
   (bundle-paths request ["app.js"]) => ["http://example.com/somewhere/main.js"]))
