(defproject optimus "0.0.1-SNAPSHOT"
  :description "A Ring middleware for frontend performance optimization."
  :url "http://github.com/magnars/catenate"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [pathetic "0.5.1"]
                 [clj-time "0.5.1"]]
  :profiles {:dev {:dependencies [[midje "1.5.0"]]
                   :resource-paths ["test/resources"]}})

