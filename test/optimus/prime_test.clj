(ns optimus.prime-test
  (:use [midje.sweet]
        [optimus.test-helper :refer [with-files]]
        [optimus.prime])
  (:import java.io.File))

(defn app-that-returns-request [request]
  request)

(fact

 (with-files [["/code.js" "1 + 2"]]
   (let [app (-> app-that-returns-request
                 (wrap-with-file-bundle "/app.js" "with-files-tmp"
                                        ["/code.js"]))]
     (app {})))

 => {:optimus-files [{:path "/code.js"
                      :original-path "/code.js"
                      :contents "1 + 2"
                      :bundle "/app.js"}]})

