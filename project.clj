(defproject optimus "2023.10.18"
  :description "A Ring middleware for frontend performance optimization."
  :url "http://github.com/magnars/optimus"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/core.memoize "0.8.2"]
                 [org.clojure/data.codec "0.1.1"]
                 [org.clojure/java.data "1.0.95"]
                 [org.clojure/data.json "2.4.0"]
                 [pathetic "0.5.1"]
                 [org.graalvm.js/js "19.3.0"]
                 [org.graalvm.js/js-scriptengine "19.3.0"]
                 [environ "1.1.0"]
                 [com.nextjournal/beholder "1.0.2"]
                 [potemkin "0.4.5"]
                 [com.cemerick/url "0.1.1"]]
  :profiles {:dev     {:dependencies   [[midje "1.9.9"]
                                        [optimus-test-jar "0.1.0"]
                                        [test-with-files "0.1.1"]]
                       :plugins        [[lein-midje "3.2.1"]
                                        [lein-shell "0.5.0"]
                                        [lein-environ "1.1.0"]]
                       :resource-paths ["test/resources"]
                       :source-paths   ["dev"]
                       :jvm-opts       ["-Djava.awt.headless=true"]}
             :rhino   {:dependencies [[cat.inspiracio/rhino-js-engine "1.7.10"]]
                       :env          {:optimus-js-engines "rhino"}}
             :nashorn {:env {:optimus-js-engines "nashorn"}}}
  :prep-tasks [["shell" "make"]])
