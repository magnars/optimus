(defproject optimus "0.9.3"
  :description "A Ring middleware for frontend performance optimization."
  :url "http://github.com/magnars/optimus"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.memoize "0.5.6"]
                 [org.clojure/data.codec "0.1.0"]
                 [pathetic "0.5.1"]
                 [clj-time "0.5.1"]
                 [clj-v8-native "0.1.4"]
                 [org.clojars.elmom/clojure-jna "0.9"]
                 [net.java.dev.jna/jna "3.5.1"]]
  :profiles {:dev {:dependencies [[midje "1.5.0"]]
                   :plugins [[lein-midje "3.0.0"]
                             [lein-shell "0.3.0"]]
                   :resource-paths ["test/resources"]}}
  :prep-tasks [["shell" "./build-js-sources.sh"]])
