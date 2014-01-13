(ns optimus.paths-test
  (:require [optimus.paths :refer :all]
            [midje.sweet :refer :all]))

(fact (just-the-path "/theme/styles/main.css") => "/theme/styles/")
(fact (just-the-filename "/theme/styles/main.css") => "main.css")
(fact (to-absolute-url "/theme/styles/main.css" "../images/bg.png") => "/theme/images/bg.png")
(fact (filename-ext "/theme/styles/main.css") => "css")
