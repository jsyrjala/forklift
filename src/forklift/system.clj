(ns forklift.system
  "TODO"
  (:require [clojure.tools.logging :refer [debug]]
            [com.stuartsierra.component :as component]
            [plumbing.core :refer [defnk]]
            [com.redbrainlabs.system-graph :as system-graph]
            [forklift.metrics :as metrics]
            )
  )

(defnk metrics []
  (metrics/map->Metrics {}))

(defnk jmx-reporter [metrics]
  (metrics/->JmxReporter metrics))

(defnk console-reporter [metrics]
  (metrics/->ConsoleReporter metrics))

(defrecord ForkliftApp [metrics]
  component/Lifecycle
  (start [this]
         (debug "ForkliftApp starting")
         this
         )
  (stop [this]
        (debug "ForkliftApp stopping")
        this
        )
  )

(defnk forklift-app [metrics]
  (->ForkliftApp metrics)
  )

(def forklift-graph
  {:metrics metrics
   ;;:console-reporter console-reporter
   :jmx-reporter jmx-reporter
   :forklift-app forklift-app
   })


(defn- create-system [graph options]
  (system-graph/init-system graph options))

(defn create-dev-system []
  (create-system forklift-graph
                 {}))
