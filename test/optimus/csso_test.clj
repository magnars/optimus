(ns optimus.csso-test
  (:require [midje.sweet :refer [=> fact]]
            [optimus.csso :as sut]))

(fact
  (sut/minify "body { color: red; }\nbody { background: black; }")
  => "body{color:red;background:#000}")

(fact
  (sut/minify "body { color: red; }\nbody { background: black; }"
              {:restructure false})
  => "body{color:red}body{background:#000}")
