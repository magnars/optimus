(ns optimus.clean-css
  (:require [clojure.java.io :as io]
            [optimus.javascript :as js])
  (:import [org.graalvm.polyglot Context]))

(def scripts
  (future [(slurp (io/resource "clean-css.js"))]))

(defn create-context []
  (js/create-context {:scripts @scripts}))

(def default-clean-css-settings
  {:level 2})

(defn create-legacy-clean-css-settings [options]
  {:level {1 {:all true
              :specialComments (:keep-special-comments options "'all'")}
           2 {:all (:advanced-optimizations options true)}}
   :format (if (:keep-line-breaks options) "keep-breaks" false)
   :compatibility (:compatibility options "*")})

(defn is-legacy-clean-css-opts? [options]
  (seq (select-keys options [:keep-special-comments :advanced-optimizations :keep-line-breaks])))

(defn get-clean-css-settings [options]
  (-> (let [options (dissoc options :aggressive-merging)]
        (if (is-legacy-clean-css-opts? options)
          (create-legacy-clean-css-settings options)
          (or (not-empty options) default-clean-css-settings)))
      (assoc :inline false)))

(defn minify
  ([css options]
   (with-open [context (create-context)]
     (minify context css options)))
  ([^Context context css options]
   (js/with-error-handling
     (let [CleanCSS (js/call-global-fn context :require "clean-css")]
       (-> (js/construct context CleanCSS options)
           (js/call-method :minify css)
           (js/get-string :styles))))))

(defn minify-css
  ([css] (minify-css css {}))
  ([css options]
   (with-open [context (create-context)]
     (minify-css context css options)))
  ([^Context context css options]
   (minify context css (get-clean-css-settings options))))

(comment

  (minify-css "body { color: red; }")

  )
