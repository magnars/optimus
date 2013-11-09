(ns optimus.wrappers-test
  (:use [optimus.wrappers]
        [optimus.test-helper]
        [midje.sweet])
  (:require [clj-time.core :as time]))

;; Adding files to the request

(with-files [["/code.js" "1 + 2"]
             ["/more.js" "3 + 5"]]

  (fact
   "wrap-with-file-bundle adds files to :optimus-files that have
    their :bundle property set. The order is preserved to ensure files
    in the bundle are loaded in the correct sequence."

   (let [app (-> app-that-returns-request
                 (wrap-with-file-bundle "/app.js" public-dir
                                        ["/code.js" "/more.js"]))]
     (app {}))

   => {:optimus-files [{:path "/code.js"
                        :original-path "/code.js"
                        :contents "1 + 2"
                        :bundle "/app.js"}
                       {:path "/more.js"
                        :original-path "/more.js"
                        :contents "3 + 5"
                        :bundle "/app.js"}]})

  (fact
   "Several bundles can be added."

   (let [app (-> app-that-returns-request
                 (wrap-with-file-bundle "/lib.js" public-dir
                                        ["/code.js"])
                 (wrap-with-file-bundle "/app.js" public-dir
                                        ["/more.js"]))]
     (set (map :bundle (:optimus-files (app {})))))

   => #{"/lib.js" "/app.js"})

  (fact
   "There's wrap-with-file-bundles to reduce verbosity."

   (let [app (-> app-that-returns-request
                 (wrap-with-file-bundles public-dir
                                         {"/lib.js" ["/code.js"]
                                          "/app.js" ["/more.js"]}))]
     (set (map :bundle (:optimus-files (app {})))))

   => #{"/lib.js" "/app.js"})

  (fact
   "You can also add individual files that are not bundled. This is
    useful for images that are served straight from the HTML."

   (let [app (-> app-that-returns-request
                 (wrap-with-files public-dir ["/code.js"]))]
     (app {}))

   => {:optimus-files [{:path "/code.js"
                        :original-path "/code.js"
                        :contents "1 + 2"}]}))

;; cache busters and expires headers

(with-redefs [time/now (fn [] (time/date-time 2013 07 30))]

  (fact
   "By adding cache busters based on content to the path, we can
     have far-future expires headers - maximizing cache time without
     worrying about stale content in the users browsers.

     We still serve the original file, but only if you ask for it
     directly. Otherwise we count it as :outdated."

   (let [app (-> app-that-returns-request
                 (wrap-with-cache-busted-expires-headers))]
     (app {:optimus-files [{:path "/code.js"
                            :original-path "/code.js"
                            :contents "1 + 2"}]}))

   => {:optimus-files [{:path "/code.js"
                        :original-path "/code.js"
                        :contents "1 + 2"
                        :outdated true}
                       {:path "/f549e6e556ea/code.js"
                        :original-path "/code.js"
                        :contents "1 + 2"
                        :headers {"Cache-Control" "max-age=315360000"
                                  "Expires" "Fri, 28 Jul 2023 00:00:00 GMT"}}]})

  #_(fact
   "The file paths in CSS files must be updated to include cache
    busters, so that they too can be served with far-future expires
    headers. There is a snag, tho:

    Consider the case where the only change is an updated image. If
    the CSS is not updated with the images' cache busting path
    before calculating its own cache buster, then the CSS file path
    will not reflect the change. And so old clients will keep on
    requesting the old image - one that is no longer served.

    In other words, we need cascading changes from referenced files
    inside CSS. We handle this by ensuring all referenced files are
    fixed first, along with updating URLs in the referencing files."

   (let [app (-> app-that-returns-request
                 (wrap-with-cache-busted-expires-headers))]
     (->> (app {:optimus-files [{:path "/main.css"
                                 :original-path "/main.css"
                                 :contents "#id1 { background: url('/bg.png'); }"
                                 :references ["/bg.png"]}
                                {:path "/bg.png"
                                 :original-path "/bg.png"
                                 :contents "binary"}]})
          :optimus-files
          (map (juxt :path :contents)))

     => [["/main.css" "#id1 { background: url('/7e57cfe84314/bg.png'); }"]
         ["/bg.png" "binary"]
         ["/0508e66b8b0d/main.css" "#id1 { background: url('/7e57cfe84314/bg.png'); }"]
         ["/7e57cfe84314/bg.png" "binary"]])))
