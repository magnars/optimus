(ns optimus.html-test
  (:require [midje.sweet :refer :all]
            [optimus.html :refer :all]))

(fact
 "You can easily link to your bundles."

 (let [request {:optimus-assets [{:path "/main.js"   :bundle "app.js"}
                                 {:path "/more.js"   :bundle "app.js"}
                                 {:path "/other.js"  :bundle "lib.js"}
                                 {:path "/reset.css" :bundle "styles.css"}
                                 {:path "/main.css"  :bundle "styles.css"}]}]

   (link-to-js-bundles request ["app.js" "lib.js"])
   => "<script src=\"/main.js\"></script><script src=\"/more.js\"></script><script src=\"/other.js\"></script>"

   (link-to-css-bundles request ["styles.css"])
   => "<link href=\"/reset.css\" rel=\"stylesheet\" /><link href=\"/main.css\" rel=\"stylesheet\" />"))
