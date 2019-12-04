(ns optimus.assets.creation-test
  (:require [midje.sweet :refer [fact]]
            [optimus.assets.creation :as sut]
            [optimus.class-path]))

(with-redefs [optimus.class-path/file-paths-on-class-path
              (fn [] ["/foo.txt"
                      "/public/"
                      "/public/foo.txt"
                      "/public/bar/foo.txt"
                      "/public/bar/bar.txt"])]
  (fact
   "realizes regexp-paths without breaking"
   (sut/realize-regex-paths "public" #".*foo.txt")
   => ["/foo.txt"
       "/bar/foo.txt"]))
