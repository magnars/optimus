(ns optimus.ph-css-test
  (:require [midje.sweet :refer [=> fact]]
            [optimus.ph-css :as sut]))

(fact
  (sut/minify "body { color: red; }\nbody { background: black; }")
  => "body{color:red}body{background:black}")

(fact
  (sut/minify ".prose { :where(h1) { font-weight: 800; } } ")
  => ".prose{:where(h1){font-weight:800}}")
