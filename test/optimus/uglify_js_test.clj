(ns optimus.uglify-js-test
  (:require [midje.sweet :refer [fact => throws]]
            [optimus.uglify-js :as sut]))

(fact
  "Minifies JS"
  (sut/minify "var hello = 2 + 3;") => "var hello=5;")

(fact
  "Minifies JS with quotes"
  (sut/minify "var hello = 'Hey';") => "var hello=\"Hey\";")

(fact
  "Minifies JS with newlines"
  (sut/minify "var hello = 'Hey' + \n 'there';") => "var hello=\"Heythere\";")

(fact
  "Minifies JS with nasty regex"
  (sut/minify "var rsingleTag = /^<(\\w+)\\s*\\/?>(?:<\\/\\1>|)$/;") => "var rsingleTag=/^<(\\w+)\\s*\\/?>(?:<\\/\\1>|)$/;")

(fact
  "Transpiles lambda notation to valid ES5 when option is enabled"
  (sut/minify "const hello = (hey) => { console.info(hey)};" {:transpile-es6? true})
  => "var hello=function(o){console.info(o)};")

(fact
  "Does not transpile when option is not enabled"
  (sut/minify "const hello = (hey) => { console.info(hey)};")
  => (throws Exception #"Unexpected token"))

(fact
  "Transpiles class definition to valied ES5"
  (sut/minify "class Test {}" {:transpile-es6? true})
  => "function _typeof(e){\"@babel/helpers - typeof\";return(_typeof=\"function\"==typeof Symbol&&\"symbol\"==typeof Symbol.iterator?function(e){return typeof e}:function(e){return e&&\"function\"==typeof Symbol&&e.constructor===Symbol&&e!==Symbol.prototype?\"symbol\":typeof e})(e)}function _defineProperties(e,t){for(var r=0;r<t.length;r++){var o=t[r];o.enumerable=o.enumerable||!1,o.configurable=!0,\"value\"in o&&(o.writable=!0),Object.defineProperty(e,_toPropertyKey(o.key),o)}}function _createClass(e,t,r){return t&&_defineProperties(e.prototype,t),r&&_defineProperties(e,r),Object.defineProperty(e,\"prototype\",{writable:!1}),e}function _toPropertyKey(e){var t=_toPrimitive(e,\"string\");return\"symbol\"===_typeof(t)?t:String(t)}function _toPrimitive(e,t){if(\"object\"!==_typeof(e)||null===e)return e;var r=e[Symbol.toPrimitive];if(void 0!==r){var o=r.call(e,t||\"default\");if(\"object\"!==_typeof(o))return o;throw new TypeError(\"@@toPrimitive must return a primitive value.\")}return(\"string\"===t?String:Number)(e)}function _classCallCheck(e,t){if(!(e instanceof t))throw new TypeError(\"Cannot call a class as a function\")}var Test=_createClass(function e(){\"use strict\";_classCallCheck(this,e)});")

(fact
  "Transpiles 'let' to valid ES5 and minifies expressions"
  (sut/minify "let apekatt = 7+3;" {:transpile-es6? true})
  => "var apekatt=10;")

(fact
  "Throws exception on syntax errors"
  (sut/minify "var hello =") => (throws Exception #"Unexpected token"))

(fact
  "Mangles names by default"
  (sut/minify "var hmm = (function () { var yoyoyo = 2; return yoyoyo; }());")
  => "var hmm=function(){var r=2;return r}();")

(fact
  "Disable name mangling"
  (sut/minify "var hmm = (function () { var yoyoyo = 2; return yoyoyo; }());" {:mangle-names false})
  => "var hmm=function(){var yoyoyo=2;return yoyoyo}();")
