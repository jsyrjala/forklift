(defproject forklift "0.1.0-SNAPSHOT"
  :description "Load performace testing tool"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :profiles {:dev {:source-paths ["dev"]
                   :dependencies
                   [[org.clojure/tools.namespace "0.2.8"]
                    [org.clojure/java.classpath "0.2.2"]
                    [midje "1.6.3" :exclusions [org.clojure/clojure]]
                    ]}
             :uberjar {:aot [forklift.main]}}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [metrics-clojure "2.4.0"]
                 [metrics-clojure-graphite "2.4.0"]
                 [ch.qos.logback/logback-classic "1.1.2"]
                 [org.clojure/tools.logging "0.3.1"]
                 [com.google.guava/guava "18.0"]
                 [slingshot "0.12.1"]
                 [prismatic/schema "0.3.3"]
                 [prismatic/plumbing "0.3.5"]
                 [com.stuartsierra/component "0.2.2"]
                 [com.redbrainlabs/system-graph "0.2.0"]
                 [clj-time "0.9.0"]
                 [org.clojure/tools.cli "0.3.1"]

                 [clj-http "1.0.1"]
                 [cheshire "5.4.0"]

                 [incanter/incanter-core "1.9.0"]
                 [incanter/incanter-charts "1.9.0"]
                 [org.flatland/ordered "1.5.2"]
                 ]
  :main forklift.main
  :jvm-opts ["-server" "-XX:+UseConcMarkSweepGC"]
  )
