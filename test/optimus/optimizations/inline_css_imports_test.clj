(ns optimus.optimizations.inline-css-imports-test
  (:require [optimus.optimizations.inline-css-imports :refer :all]
            [midje.sweet :refer :all]))

(fact
 "CSS imports are inlined from the asset list, and removed from the
  references."

 (inline-css-imports [{:path "/main.css" :contents "@import '/other.css'; #id {}" :references #{"/other.css"}}
                      {:path "/other.css" :contents ".class {}"}])
 => [{:path "/main.css" :contents ".class {} #id {}"}
     {:path "/other.css" :contents ".class {}"}])

(fact
 "CSS imports can take many forms."

 (let [assets [{:path "/main.css" :contents "@import '/other.css';"}
               {:path "/other.css" :contents ".class {}"}]
       expected [{:path "/main.css" :contents ".class {}"}
                 {:path "/other.css" :contents ".class {}"}]]

   (inline-css-imports (assoc-in assets [0 :contents] "@import '/other.css';")) => expected
   (inline-css-imports (assoc-in assets [0 :contents] "@import \"/other.css\";")) => expected
   (inline-css-imports (assoc-in assets [0 :contents] "@import url('/other.css');")) => expected
   (inline-css-imports (assoc-in assets [0 :contents] "@import url(\"/other.css\");")) => expected
   (inline-css-imports (assoc-in assets [0 :contents] "@import url(/other.css);")) => expected))

(fact
 "Media queries are conserved."

 (inline-css-imports [{:path "/main.css" :contents "@import '/other.css' screen and (orientation:landscape);"}
                      {:path "/other.css" :contents ".class {}"}])
 => [{:path "/main.css" :contents "@media screen and (orientation:landscape) { .class {} }"}
     {:path "/other.css" :contents ".class {}"}])

(fact
 "External URLs for @imports are not tolerated. It's disastrous for frontend performance."

 (inline-css-imports [{:path "/main.css" :contents "@import 'http://external.css';"}])
 => (throws Exception "Import of external URL http://external.css in /main.css is strongly adviced against. It's a performance killer. In fact, there's no option to allow this. Use a link in your HTML instead. Open an issue if you really, really need it."))

(fact
 "Non-CSS assets are left to their own devices."

 (inline-css-imports [{:path "/main.js" :contents "@import '/other.css';"}])
 => [{:path "/main.js" :contents "@import '/other.css';"}])
