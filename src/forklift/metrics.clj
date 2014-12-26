(ns forklift.metrics
  (:require
   [clojure.tools.logging :refer [info debug error]]
   [com.stuartsierra.component :refer [Lifecycle]]
   [metrics.core :refer [new-registry]]
   [slingshot.slingshot :refer [try+ throw+]]
   [metrics.reporters.console :as console]
   [metrics.reporters.jmx :as jmx]
   ))

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

(defrecord Metrics []
  Lifecycle
  (start [this]
         (debug "Metrics starting")
         (assoc this :registry (new-registry)))
  (stop [this]
        (debug "Metrics stopping")
        (dissoc this :registry)
        ))



