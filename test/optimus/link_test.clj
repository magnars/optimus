(ns optimus.link-test
  (:use [optimus.link]
        [midje.sweet]))

(fact
 "You can link to a specific file by its original path. Outdated files
  are skipped."

 (let [request {:optimus-assets [{:path "/bg.png"}
                                 {:path "/main.js" :outdated true}
                                 {:path "/123/m.js" :original-path "/main.js"}]}]

   (file-path request "/bg.png") => "/bg.png"
   (file-path request "/main.js") => "/123/m.js"))
