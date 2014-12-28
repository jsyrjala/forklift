(ns forklift.metrics
  (:require
   [clojure.tools.logging :refer [info debug error]]
   [com.stuartsierra.component :refer [Lifecycle]]
   [metrics.core :refer [new-registry]]
   [slingshot.slingshot :refer [try+ throw+]]
   [metrics.reporters.console :as console]
   [metrics.reporters.jmx :as jmx]
   [metrics.reporters.csv :as csv]
   [metrics.reporters.graphite :as graphite])
  (:import [java.util.concurrent TimeUnit])
   )

(defrecord JmxReporter [metrics]
  Lifecycle
  (start [this]
         (debug "JmxReporter starting")
         (let [registry (-> metrics :registry)
               reporter (jmx/reporter registry {})
               ]
           (jmx/start reporter)
           (assoc this :jmx-reporter reporter))
         )

  (stop [this]
        (debug "JmxReporter stopping")
        (-> this :jmx-reporter jmx/stop)
        (dissoc this :jmx-reporter))
  )

(defrecord ConsoleReporter [metrics]
  Lifecycle
  (start [this]
         (debug "ConsoleReporter starting")

         (let [registry (-> metrics :registry)
               reporter (console/reporter registry {})
               ]
           (console/start reporter 10)
           (assoc this :console-reporter reporter)
           ))
  (stop [this]
        (debug "ConsoleReporter stopping")
        (-> this :console-reporter console/stop)
        (dissoc this :console-reporter)
        )
  )

(defrecord CsvReporter [metrics]
  Lifecycle
  (start [this]
         (debug "CsvReporter starting")

         (let [registry (-> metrics :registry)
               reporter (csv/reporter registry "log/csv-reporter" {})
               ]
           (csv/start reporter 1)
           (assoc this :csv-reporter reporter)
           ))
  (stop [this]
        (debug "CsvReporter stopping")
        (-> this :console-reporter csv/stop)
        (dissoc this :csv-reporter)
        )
  )

(defrecord GraphiteReporter [metrics]
  Lifecycle
  (start [this]
         (debug "GraphiteReporter starting")

         (let [registry (-> metrics :registry)
               reporter (graphite/reporter registry
                                           {:host "localhost"
                                            :prefix "forklift"
                                            :rate-unit TimeUnit/SECONDS
                                            :duration-unit TimeUnit/MILLISECONDS})
               ]
           (graphite/start reporter 1)
           (assoc this :graphite-reporter reporter)
           ))
  (stop [this]
        (debug "GraphiteReporter stopping")
        (-> this :graphite-reporter graphite/stop)
        (dissoc this :graphite-reporter)
        )
  )



(defrecord Metrics []
  Lifecycle
  (start [this]
         (debug "Metrics starting")
         (assoc this :registry (new-registry)))
  (stop [this]
        (debug "Metrics stopping")
        (dissoc this :registry)
        ))



