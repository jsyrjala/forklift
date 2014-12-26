(ns forklift.load-tester
  "TODO"
  (:require
   [clojure.tools.logging :refer [info debug error]]
   [com.stuartsierra.component :refer [Lifecycle]]
   [forklift.data :as forklift]
   )
  )


(defprotocol ILoaderFactory
  ""
  (create-loader [this opts suite running])
  )

(defrecord LoaderFactory [metrics]
  Lifecycle
  ILoaderFactory
  (create-loader
   [this opts suite running]

   )
  (start [this] this)
  (stop [this] this)
  )

(defprotocol ILoadTester
  (start-load-test [this load-suite]))


(defrecord LoadTester [metrics]
  Lifecycle
  ILoadTester
  (start
   [this]
   (debug "LoadTester starting")
   (let [running (atom true)]
     (assoc this :running running)
     ))
  (start-load-test
   [this suites]
   (let [running (-> this :running)]
     (debug "start load test" running)
     (forklift/run-load {:running running
                         :metrics metrics} suites)
     ))
  (stop
   [this]
   (debug "LoadTester stopping")
   (reset! (-> this :running) false)
   (dissoc this :running)
   )
  )
