(ns optimus.optimizations.minify-test
  (:require [midje.sweet :refer [fact => throws]]
            [optimus.optimizations.minify :as sut]))

(fact
 "Minifies JS"
 (sut/minify-js "var hello = 2 + 3;") => "var hello=5;")

(fact
 "It minifies a list of JS assets."
 (sut/minify-js-assets [{:path "code.js" :contents "var a = 2 + 3;"}
                    {:path "more.js" :contents "var b = 4 + 5;"}])
 => [{:path "code.js" :contents "var a=5;"}
     {:path "more.js" :contents "var b=9;"}])

(fact
 "It only minifies .js files"
 (sut/minify-js-assets [{:path "code.js" :contents "var a = 2 + 3;"}
                    {:path "styles.css" :contents "#id { margin: 0; }"}])
 => [{:path "code.js" :contents "var a=5;"}
     {:path "styles.css" :contents "#id { margin: 0; }"}])

(fact
 "It passes options along."
 (sut/minify-js-assets [{:path "unmangled.js"
                     :contents "var hmm = (function () { var yoyoyo = 2; return yoyoyo; }());"}]
                   {:uglify-js {:mangle-names false}})
 => [{:path "unmangled.js"
      :contents "var hmm=function(){var yoyoyo=2;return yoyoyo}();"}])

(fact
 "It includes the path in exception."
 (sut/minify-js-assets [{:path "/the-path/code.js" :contents "var hello ="}])
 => (throws Exception #"/the-path/code.js"))

;; minify CSS

(fact
 "It skips minification of css files with very long one-liners. It's a decent
  heuristic that it's already minified."
 (let [css {:path "styles.css"
            :contents (str "/* comment */\nbody {" (apply str (repeat 500 "color:red;")) "}")}]
   (sut/minify-css-assets [css]) => [css]))

(fact
 "It minifies a list of CSS assets."
 (sut/minify-css-assets [{:path "reset.css" :contents "body { color: red; }"}
                         {:path "style.css" :contents "body { color: #ffff00; }"}])
 => [{:path "reset.css" :contents "body{color:red}"}
     {:path "style.css" :contents "body{color:#ff0}"}])

(fact
 "It only minifies .css files"
 (sut/minify-css-assets [{:path "code.js" :contents "var a = 2 + 3;"}
                     {:path "styles.css" :contents "#id { margin: 0; }"}])
 => [{:path "code.js" :contents "var a = 2 + 3;"}
     {:path "styles.css" :contents "#id{margin:0}"}])
