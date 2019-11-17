(ns optimus.digest-test
  (:use optimus.digest
        midje.sweet)
  (:require [clojure.java.io :as io]))

(fact (sha-1 "abc") => "a9993e364706816aba3e25717850c26c9cd0d89d")
(fact (sha-1 "def") => "589c22335a381f122d129225f5c0ba3056ed5811")

(def blank-gif (slurp (io/resource "blank.gif") :encoding "UTF-8"))

(fact (base64-string "hello world") => "aGVsbG8gd29ybGQ=")
(fact (base64-string blank-gif) => "R0lGODlhAQABAO+/vQAAAAAA77+977+977+9Ie+/vQQBAAAAACwAAAAAAQABAAACAUQAOw==")
