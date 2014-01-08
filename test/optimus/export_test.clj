(ns optimus.export-test
  (:require [optimus.export :refer [save-assets]]
            [test-with-files.core :refer [with-tmp-dir tmp-dir]]
            [clojure.java.io :as io]
            [midje.sweet :refer [fact =>]]))

(fact
 "You can save assets to disk, like if you need to push them to a CDN server."

 (with-tmp-dir
   (save-assets [{:path "/code.js" :contents "var a = 1 + 2;"}] tmp-dir)
   (slurp (str tmp-dir "/code.js")) => "var a = 1 + 2;"))

(fact
 "The same goes for binary files."

 (with-tmp-dir
   (save-assets [{:path "/blank.gif" :resource (io/resource "blank.gif")}] tmp-dir)
   (slurp (str tmp-dir "/blank.gif")) => (slurp (io/resource "blank.gif"))))

(fact
 "It handles nested folder structures"

 (with-tmp-dir
   (save-assets [{:path "/theme/css/main.css" :contents "yep"}] tmp-dir)
   (slurp (str tmp-dir "/theme/css/main.css")) => "yep"))
