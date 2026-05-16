(ns optimus.ph-css-test
  (:require [midje.sweet :refer [=> fact]]
            [optimus.ph-css :as sut]))

(fact
  (sut/minify "body { color: red; }\nbody { background: black; }")
  => "body{color:red}body{background:black}")

(fact
  (sut/minify ".prose { :where(h1) { font-weight: 800; } } ")
  => ".prose{:where(h1){font-weight:800}}")

(fact
  "It sets options without problems"
  (sut/minify "#blåbærsyltetøy { padding: 10px 10px 10px 10px; }"
              {:optimized-output? true
               :remove-unnecessary-code? true
               :indent 0
               :quote-urls? true
               :write-namespace-rules? true
               :write-nested-declarations? true
               :write-font-face-rules? true
               :write-key-frames-rules? true
               :write-layer-rules? true
               :write-media-rules? true
               :write-page-rules? true
               :write-viewport-rules? true
               :write-supports-rules? true
               :write-property-rules? true
               :write-unknown-rules? true})
  => "#blåbærsyltetøy{padding:10px 10px 10px 10px}")
