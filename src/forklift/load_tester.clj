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

(defn- running-creator [duration]
  (let [start (System/currentTimeMillis)
        endtime (+ start duration)]
    (fn []
      (let [now (System/currentTimeMillis)]
        (if (> endtime now)
          true
          (do
            (info "Stopping load generation. Forklift has been running over timelimit" duration "milliseconds")
            false)
          )
        ))))

(defrecord LoadTester [metrics]
  Lifecycle
  ILoadTester
  (start
   [this]
   (debug "LoadTester starting")
   (let [registry (-> metrics :registry)
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
     (assoc this :stats stats)
     ))
  (start-load-test
   [this suite-config]
   (let [{:keys [stats]} this
         {:keys [duration
                 before-fn
                 after-fn
                 suites]} suite-config
         running-fn (running-creator duration)]
     ;; TODO handle exceptions
     (when before-fn
       (before-fn suite-config))
     (let [loaders (forklift/run-load {:running-fn running-fn
                                       :metrics metrics
                                       :stats stats}
                                      suite-config)]
       (doall (map deref loaders)))
     ;; TODO handle exceptions
     (when after-fn
       ;; TODO exec threads are still running at this point
       ;; TODO change loaders to wait for all sub threads to finish
       (after-fn suite-config))
     ))
  (stop
   [this]
   (debug "LoadTester stopping")
   (reset! (-> this :running) false)
   (dissoc this :running)
   )
  )
