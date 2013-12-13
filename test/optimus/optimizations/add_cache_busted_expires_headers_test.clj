(ns optimus.optimizations.add-cache-busted-expires-headers-test
  (:use [optimus.optimizations.add-cache-busted-expires-headers]
        [midje.sweet])
  (:require [clj-time.core :as time])
  (:import [java.io ByteArrayInputStream]))

(with-redefs [time/now (fn [] (time/date-time 2013 07 30))]

  (fact
   "By adding cache busters based on content to the path, we can
    have far-future expires headers - maximizing cache time without
    worrying about stale content in the users browsers.

    We still serve the original file, but only if you ask for it
    directly. Otherwise we count it as :outdated."

   (add-cache-busted-expires-headers [{:path "/code.js"
                                       :contents "1 + 2"}])
   => [{:path "/code.js"
        :contents "1 + 2"
        :outdated true}
       {:path "/f549e6e556ea/code.js"
        :original-path "/code.js"
        :contents "1 + 2"
        :headers {"Expires" "Fri, 28 Jul 2023 00:00:00 GMT"}}])

  (fact
   "While it's important that the :original-path property is set, so
   that we can find the file again later, it shouldn't overwrite one
   that is there already."

   (->> (add-cache-busted-expires-headers [{:path "/c.js"
                                            :original-path "/code.js"
                                            :contents "1 + 2"}])
        (map (juxt :path :original-path)))
   => [["/c.js" "/code.js"]
       ["/f549e6e556ea/c.js" "/code.js"]])

  (fact
   "It shouldn't overwrite other headers that are there either."

   (->> (add-cache-busted-expires-headers [{:path "/c.js"
                                            :headers {"Last-Modified" "Fri, 28 Jul 2023 00:00:00 GMT"}
                                            :contents "1 + 2"}])
        (map (juxt :path :headers)))
   => [["/c.js" {"Last-Modified" "Fri, 28 Jul 2023 00:00:00 GMT"}]
       ["/f549e6e556ea/c.js" {"Last-Modified" "Fri, 28 Jul 2023 00:00:00 GMT"
                              "Expires" "Fri, 28 Jul 2023 00:00:00 GMT"}]])

  (fact
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

   (->> (add-cache-busted-expires-headers [{:path "/main.css" :contents "#id1 { background: url('/bg.png'); }" :references #{"/bg.png"}}
                                           {:path "/bg.png" :get-stream #(ByteArrayInputStream. (.getBytes "binary"))}])
        (map (juxt :path :contents :references)))

   => [["/main.css" "#id1 { background: url('/bg.png'); }" #{"/bg.png"}]
       ["/bg.png" nil nil]
       ["/0508e66b8b0d/main.css" "#id1 { background: url('/7e57cfe84314/bg.png'); }" #{"/7e57cfe84314/bg.png"}]
       ["/7e57cfe84314/bg.png" nil nil]]))
