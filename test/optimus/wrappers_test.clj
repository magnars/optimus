(ns optimus.wrappers-test
  (:use [midje.sweet]
        [optimus.test-helper]
        [optimus.wrappers]))

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

