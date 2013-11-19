(ns optimus.hiccup-test
  (:use [optimus.hiccup]
        [midje.sweet]))

(fact
 "If you use hiccup, you can easily link to your bundles."

 (let [request {:optimus-assets [{:path "/main.js"   :bundle "app.js"}
                                 {:path "/more.js"   :bundle "app.js"}
                                 {:path "/other.js"  :bundle "lib.js"}
                                 {:path "/reset.css" :bundle "styles.css"}
                                 {:path "/main.css"  :bundle "styles.css"}]}]

   (link-to-js-bundles request ["app.js" "lib.js"])
   => [[:script {:src "/main.js"}]
       [:script {:src "/more.js"}]
       [:script {:src "/other.js"}]]

   (link-to-css-bundles request ["styles.css"])
   => [[:link {:rel "stylesheet" :href "/reset.css"}]
       [:link {:rel "stylesheet" :href "/main.css"}]]))
