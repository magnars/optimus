(ns optimus.minify-test
  (:require [optimus.minify :refer [minify-js]]
            [midje.sweet :refer [fact =>]]))

(fact (minify-js "var hello = 2 + 3;") => "var hello=5;")
