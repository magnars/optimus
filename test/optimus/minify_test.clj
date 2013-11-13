(ns optimus.minify-test
  (:require [optimus.minify :refer [minify-js minify-css]]
            [midje.sweet :refer [fact => throws]]))

(fact
 "Minifies JS"
 (minify-js "var hello = 2 + 3;") => "var hello=5;")

(fact
 "Minifies JS with quotes"
 (minify-js "var hello = 'Hey';") => "var hello=\"Hey\";")

(fact
 "Minifies JS with newlines"
 (minify-js "var hello = 'Hey' + \n 'there';") => "var hello=\"Heythere\";")

(fact
 "Throws exception on syntax errors"
 (minify-js "var hello = ") => (throws Exception))

(fact
 "Mangles names by default"
 (minify-js "var hmm = (function () { var yoyoyo = 2; return yoyoyo; }());") =>
 "var hmm=function(){var r=2;return r}();")

(fact
 "Disable name mangling"
 (minify-js "var hmm = (function () { var yoyoyo = 2; return yoyoyo; }());" {:mangle-names false}) =>
 "var hmm=function(){var yoyoyo=2;return yoyoyo}();")

(fact (minify-css "body { color: red; }") => "body{color:red}")
(fact (minify-css "body {\n    color: red;\n}") => "body{color:red}")
(fact (minify-css "body {\n    color: red") => (throws Exception))
