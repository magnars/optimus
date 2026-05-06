(ns optimus.clean-css-test
  (:require [midje.sweet :refer [fact =>]]
            [optimus.clean-css :as sut]))

(fact (sut/minify-css "body { color: red; }") => "body{color:red}")
(fact (sut/minify-css "body {\n    color: red;\n}") => "body{color:red}")

(fact
 "You can turn off advanced optimizations."

 (sut/minify-css ".one{padding:0}.two{margin:0}.one{margin-bottom:3px}")
 => ".two{margin:0}.one{padding:0;margin-bottom:3px}"

 (sut/minify-css ".one{padding:0}.two{margin:0}.one{margin-bottom:3px}" {:advanced-optimizations false})
 => ".one{padding:0}.two{margin:0}.one{margin-bottom:3px}"

 (sut/minify-css ".one{padding:0}.two{margin:0}.one{margin-bottom:3px}" {:level 1})
 => ".one{padding:0}.two{margin:0}.one{margin-bottom:3px}")

(fact
 "You can keep line-breaks."

 (sut/minify-css "body{color:red}\nhtml{color:#00f}")
 => "body{color:red}html{color:#00f}"

 (sut/minify-css "body{color:red}\nhtml{color:#00f}" {:keep-line-breaks true})
 => "body{color:red}\nhtml{color:#00f}")

(fact
  "You can control special comments."

  (sut/minify-css "/*! comment */\nbody{color:red}")
  => "/*! comment */body{color:red}"

  (sut/minify-css "/*! comment */\nbody{color:red}" {:keep-special-comments 0})
  => "body{color:red}")

(fact
  "You can control compatibility mode."

  (sut/minify-css "body{margin:0px 0rem}")
  => "body{margin:0}"

  (sut/minify-css "body{margin:0px 0rem}" {:compatibility "ie7"})
  => "body{margin:0 0rem}")

(fact
 "It doesn't mess up percentages after rgb-colors."

 (sut/minify-css "body { background: -webkit-linear-gradient(bottom, rgb(209,209,209) 10%, rgb(250,250,250) 55%);}")
 => "body{background:-webkit-linear-gradient(bottom,#d1d1d1 10%,#fafafa 55%)}")

(fact
 "It doesn't mess up variable names."

 (sut/minify-css "body { background: magenta; color: var(--light-magenta); }")
 => "body{background:#ff00ff;color:var(--light-magenta)}")

(fact
  "It doesn't mess up media queries."
  (sut/minify-css "@media screen and (orientation:landscape) {#id{color:red}}")
  => "@media screen and (orientation:landscape){#id{color:red}}"

  (sut/minify-css "@import url(abc.css) screen and (min-width:7) and (max-width:9);")
  => "@import url(abc.css) screen and (min-width:7) and (max-width:9);")

(fact
  "It correctly minifies several rules for the same selector"
  (sut/minify-css "table,div {border:0} table {margin:0}" {})
  => "div,table{border:0}table{margin:0}")

(fact
  "doesn't remove transition property with never features"
  (sut/minify-css "#menu{transform:translateX(-100%);transition:transform .25s ease-in,overlay .25s allow-discrete,display .25s allow-discrete;overflow-y:scroll}" {})
  => "#menu{transform:translateX(-100%);transition:transform .25s ease-in,overlay .25s allow-discrete,display .25s allow-discrete;overflow-y:scroll}")
