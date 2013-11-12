(ns optimus.minify-test
  (:require [optimus.minify :refer [minify-js minify-css]]
            [midje.sweet :refer [fact => throws]]))

(fact (minify-js "var hello = 2 + 3;") => "var hello=5;")
(fact (minify-js "var hello = 'Hey';") => "var hello=\"Hey\";")
(fact (minify-js "var hello = 'Hey' + \n 'there';") => "var hello=\"Heythere\";")
(fact (minify-js "var hello = ") => (throws Exception))

(fact (minify-css "body { color: red; }") => "body{color:red}")
(fact (minify-css "body {\n    color: red;\n}") => "body{color:red}")
(fact (minify-css "body {\n    color: red") => (throws Exception))
