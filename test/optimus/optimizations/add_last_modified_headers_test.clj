(ns optimus.optimizations.add-last-modified-headers-test
  (:require [optimus.optimizations.add-last-modified-headers :refer :all]
            [midje.sweet :refer :all]))

(fact
 "Last-Modified headers help the browsers determine if the files are
  the same. If the Last-Modified date is sufficiently far enough in
  the past, chances are the browser won't refetch it."

 (add-last-modified-headers [{:path "/code.js"
                              :contents "1 + 2"
                              :last-modified 1375142400000}])
 => [{:path "/code.js"
      :contents "1 + 2"
      :last-modified 1375142400000
      :headers {"Last-Modified" "Tue, 30 Jul 2013 00:00:00 GMT"}}])

(fact
 "Assets with no :last-modified attribute is left unchanged."

 (add-last-modified-headers [{:path "/code.js"
                              :contents "1 + 2"}])
 => [{:path "/code.js"
      :contents "1 + 2"}])
