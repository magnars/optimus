(ns optimus.optimizations.minify-test
  (:require [optimus.optimizations.minify :refer :all]
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
 "Transpiles lambda notation to valid ES5"
 (minify-js "const hello = (hey) => { console.info(hey)};")
 => "var hello=function(o){console.info(o)};")

(fact
 "Transpiles class definition to valied ES5"
 (minify-js "class Test {}")
 => "function _typeof(e){\"@babel/helpers - typeof\";return(_typeof=\"function\"==typeof Symbol&&\"symbol\"==typeof Symbol.iterator?function(e){return typeof e}:function(e){return e&&\"function\"==typeof Symbol&&e.constructor===Symbol&&e!==Symbol.prototype?\"symbol\":typeof e})(e)}function _defineProperties(e,t){for(var r=0;r<t.length;r++){var o=t[r];o.enumerable=o.enumerable||!1,o.configurable=!0,\"value\"in o&&(o.writable=!0),Object.defineProperty(e,_toPropertyKey(o.key),o)}}function _createClass(e,t,r){return t&&_defineProperties(e.prototype,t),r&&_defineProperties(e,r),Object.defineProperty(e,\"prototype\",{writable:!1}),e}function _toPropertyKey(e){var t=_toPrimitive(e,\"string\");return\"symbol\"===_typeof(t)?t:String(t)}function _toPrimitive(e,t){if(\"object\"!==_typeof(e)||null===e)return e;var r=e[Symbol.toPrimitive];if(void 0!==r){var o=r.call(e,t||\"default\");if(\"object\"!==_typeof(o))return o;throw new TypeError(\"@@toPrimitive must return a primitive value.\")}return(\"string\"===t?String:Number)(e)}function _classCallCheck(e,t){if(!(e instanceof t))throw new TypeError(\"Cannot call a class as a function\")}var Test=_createClass(function e(){\"use strict\";_classCallCheck(this,e)});")

(fact
 "Transpiles 'let' to valid ES5 and minifies expressions"
 (minify-js "let apekatt = 7+3;")
 => "var apekatt=10;")

(fact
 "Throws exception on syntax errors"
 (minify-js "var hello =") => (throws Exception "Unexpected token: eof (undefined) (line 1, col 11)"))

(fact
 "Mangles names by default"
 (minify-js "var hmm = (function () { var yoyoyo = 2; return yoyoyo; }());")
 => "var hmm=function(){var r=2;return r}();")

(fact
 "Disable name mangling"
 (minify-js "var hmm = (function () { var yoyoyo = 2; return yoyoyo; }());" {:uglify-js {:mangle-names false}})
 => "var hmm=function(){var yoyoyo=2;return yoyoyo}();")

(fact
 "To save some time minifying a lot of files, we can create the
  uglify.JS context up front, and then reuse that for all the assets."
 (let [eng (prepare-uglify-engine)]
   (minify-js eng "var hello = 2 + 3;" {}) => "var hello=5;"
   (minify-js eng "var hello = 3 + 4;" {}) => "var hello=7;"))

(fact
 "It minifies a list of JS assets."
 (minify-js-assets [{:path "code.js" :contents "var a = 2 + 3;"}
                    {:path "more.js" :contents "var b = 4 + 5;"}])
 => [{:path "code.js" :contents "var a=5;"}
     {:path "more.js" :contents "var b=9;"}])

(fact
 "It only minifies .js files"
 (minify-js-assets [{:path "code.js" :contents "var a = 2 + 3;"}
                    {:path "styles.css" :contents "#id { margin: 0; }"}])
 => [{:path "code.js" :contents "var a=5;"}
     {:path "styles.css" :contents "#id { margin: 0; }"}])

(fact
 "It passes options along."
 (minify-js-assets [{:path "unmangled.js"
                     :contents "var hmm = (function () { var yoyoyo = 2; return yoyoyo; }());"}]
                   {:uglify-js {:mangle-names false}})
 => [{:path "unmangled.js"
      :contents "var hmm=function(){var yoyoyo=2;return yoyoyo}();"}])

(fact
 "It includes the path in exception."
 (minify-js-assets [{:path "code.js" :contents "var hello ="}])
 => (throws Exception "Exception in code.js: Unexpected token: eof (undefined) (line 1, col 11)"))

;; minify CSS

(fact (minify-css "body { color: red; }") => "body{color:red}")
(fact (minify-css "body {\n    color: red;\n}") => "body{color:red}")

(comment ;; clean-css doesn't throw exceptions for mangled CSS - see https://github.com/jakubpawlowicz/clean-css/issues/449
  (fact (minify-css "body {\n    color: red") => (throws Exception "Please check the validity of the CSS block starting from the line #1"))

  (fact
   "It includes the path in exception."
   (minify-css-assets [{:path "styles.css" :contents "body {\n    color: red"}])
   => (throws Exception "Exception in styles.css: Please check the validity of the CSS block starting from the line #1")))

(fact
 "You can turn off advanced optimizations."

 (minify-css "body { padding: 10px 10px; }")
 => "body{padding:10px}"

 (minify-css "body { padding: 10px 10px; }" {:clean-css {:advanced-optimizations false}})
 => "body{padding:10px 10px}")

(fact
 "You can turn off aggressive merging."

 (minify-css "a{display:inline-block;color:red;display:-moz-block}")
 => "a{color:red;display:-moz-block}"

 (minify-css "a{display:inline-block;color:red;display:-moz-block}" {:clean-css {:aggressive-merging false}})
 => "a{display:inline-block;color:red;display:-moz-block}")

(fact
 "You can keep line-breaks."

 (minify-css "body{color:red}\nhtml{color:#00f}") => "body{color:red}html{color:#00f}"
 (minify-css "body{color:red}\nhtml{color:#00f}" {:clean-css {:keep-line-breaks true}}) => "body{color:red}\nhtml{color:#00f}")

(fact
 "You can control special comments."

 (minify-css "/*! comment */\nbody{color:red}") => "/*! comment */body{color:red}"
 (minify-css "/*! comment */\nbody{color:red}" {:clean-css {:keep-special-comments 0}}) => "body{color:red}")

(fact
 "You can control compatibility mode."

 (minify-css "body{margin:0px 0rem}") => "body{margin:0}"
 (minify-css "body{margin:0px 0rem}" {:clean-css {:compatibility "ie7"}}) => "body{margin:0 0rem}")

(fact
 "It doesn't mess up percentages after rgb-colors."

 (minify-css "body { background: -webkit-linear-gradient(bottom, rgb(209,209,209) 10%, rgb(250,250,250) 55%);}")
 => "body{background:-webkit-linear-gradient(bottom,#d1d1d1 10%,#fafafa 55%)}")

(fact
 "It skips minification of css files with very long one-liners. It's a decent
  heuristic that it's already minified."
 (let [css (str "/* comment */\nbody {" (apply str (repeat 500 "color:red;")) "}")]
   (minify-css css) => css))

(fact
 "It doesn't mess up media queries."
 (minify-css "@media screen and (orientation:landscape) {#id{color:red}}") => "@media screen and (orientation:landscape){#id{color:red}}"
 (minify-css "@import 'abc.css' screen and (min-width:7) and (max-width:9);") => "@import 'abc.css' screen and (min-width:7) and (max-width:9);")

(fact
 "It minifies a list of CSS assets."
 (minify-css-assets [{:path "reset.css" :contents "body { color: red; }"}
                     {:path "style.css" :contents "body { color: #ffff00; }"}])
 => [{:path "reset.css" :contents "body{color:red}"}
     {:path "style.css" :contents "body{color:#ff0}"}])

(fact
 "It only minifies .css files"
 (minify-css-assets [{:path "code.js" :contents "var a = 2 + 3;"}
                     {:path "styles.css" :contents "#id { margin: 0; }"}])
 => [{:path "code.js" :contents "var a = 2 + 3;"}
     {:path "styles.css" :contents "#id{margin:0}"}])

(fact
  "It correctly minifies several rules for the same selector"
  (minify-css (prepare-clean-css-engine) "table,div {border:0} table {margin:0}" {}) => "div,table{border:0}table{margin:0}")
