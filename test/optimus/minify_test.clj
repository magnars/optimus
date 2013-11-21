(ns optimus.minify-test
  (:require [optimus.minify :refer :all]
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
 "Minifies JS with nasty regex"
 (minify-js "var rsingleTag = /^<(\\w+)\\s*\\/?>(?:<\\/\\1>|)$/;") => "var rsingleTag=/^<(\\w+)\\s*\\/?>(?:<\\/\\1>|)$/;")

(fact
 "Throws exception on syntax errors"
 (minify-js "var hello = ") => (throws Exception "Unexpected token: eof (undefined) (line 1, col 12)"))

(fact
 "Mangles names by default"
 (minify-js "var hmm = (function () { var yoyoyo = 2; return yoyoyo; }());")
 => "var hmm=function(){var r=2;return r}();")

(fact
 "Disable name mangling"
 (minify-js "var hmm = (function () { var yoyoyo = 2; return yoyoyo; }());" {:mangle-js-names false})
 => "var hmm=function(){var yoyoyo=2;return yoyoyo}();")

(fact
 "To save some time minifying a lot of files, we can create the
  uglify.JS context up front, and then reuse that for all the assets."
 (let [cx (create-uglify-context)]
   (minify-js cx "var hello = 2 + 3;" {}) => "var hello=5;"
   (minify-js cx "var hello = 3 + 4;" {}) => "var hello=7;"))

(fact
 "It minifies a list of assets."
 (minify-js-assets [{:path "code.js" :contents "var a = 2 + 3;"}
                    {:path "more.js" :contents "var b = 4 + 5;"}])
 => [{:path "code.js" :contents "var a=5;"}
     {:path "more.js" :contents "var b=9;"}])

(fact
 "It only minifies .js files"
 (minify-js-assets [{:path "code.js" :contents "var a = 2 + 3;"}
                    {:path "styles.css" :contents "#id {margin: 0}"}])
 => [{:path "code.js" :contents "var a=5;"}
     {:path "styles.css" :contents "#id {margin: 0}"}])

(fact
 "It passes options along."
 (minify-js-assets [{:path "unmangled.js"
                     :contents "var hmm = (function () { var yoyoyo = 2; return yoyoyo; }());"}]
                   {:mangle-js-names false})
 => [{:path "unmangled.js"
      :contents "var hmm=function(){var yoyoyo=2;return yoyoyo}();"}])

(fact
 "It includes the path in exception."
 (minify-js-assets [{:path "code.js" :contents "var hello = "}])
 => (throws Exception "Exception in code.js: Unexpected token: eof (undefined) (line 1, col 12)"))

;; minify CSS

(fact (minify-css "body { color: red; }") => "body{color:red}")
(fact (minify-css "body {\n    color: red;\n}") => "body{color:red}")
(fact (minify-css "body {\n    color: red") => (throws Exception "Please check the validity of the CSS block starting from the line #1"))
