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
                 [co.paralleluniverse/pulsar "0.6.2"]
                 [metrics-clojure "2.4.0"]
                 [ch.qos.logback/logback-classic "1.1.2"]
                 [org.clojure/tools.logging "0.3.1"]
                 ]
  :main forklift.main
  :jvm-opts ["-server" "-XX:+UseConcMarkSweepGC"]
  :java-agents [[co.paralleluniverse/quasar-core "0.6.2"]]
  )
