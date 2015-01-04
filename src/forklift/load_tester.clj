(ns forklift.load-tester
  "TODO"
  (:require
   [clojure.tools.logging :refer [info debug error]]
   [com.stuartsierra.component :refer [Lifecycle]]
   [forklift.data :as forklift]
   [metrics.gauges :refer [gauge-fn]]
   )
  )


(defprotocol ILoaderFactory
  "TODO experimental"
  (create-loader [this opts suite running])
  )

;; "TODO experimental"
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
   (let [registry (-> metrics :registry)
         running (atom true)
         stats (atom {:scenarios
                      {:concurrent 0
                       :started 0
                       :finished 0}})]
     (gauge-fn registry ["global" "scenarios" "concurrent"]
               (fn [] (-> @stats :scenarios :concurrent)))
     (gauge-fn registry ["global" "scenarios" "started"]
               (fn [] (-> @stats :scenarios :started)))
     (gauge-fn registry ["global" "scenarios" "finished"]
               (fn [] (-> @stats :scenarios :finished)))
     (assoc this :running-fn (fn [] @running)
       :stats stats)
     ))
  (start-load-test
   [this suites]
   (let [{:keys [running-fn
                 stats]} this]
     (forklift/run-load {:running-fn running-fn
                         :metrics metrics
                         :stats stats}
                        suites)
     ))
  (stop
   [this]
   (debug "LoadTester stopping")
   (reset! (-> this :running) false)
   (dissoc this :running)
   )
  )
