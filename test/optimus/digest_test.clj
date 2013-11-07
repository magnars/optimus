(ns optimus.digest-test
  (:use optimus.digest
        midje.sweet))

(fact (sha-1 "abc") => "a9993e364706816aba3e25717850c26c9cd0d89d")
(fact (sha-1 "def") => "589c22335a381f122d129225f5c0ba3056ed5811")
