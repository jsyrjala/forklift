(ns forklift.data
  (:require [slingshot.slingshot :refer [try+ throw+]]
            [clojure.tools.logging :refer [trace debug info error]]
            [metrics.meters :as meters]
            [metrics.timers :as timers]
            )
  (:import [com.google.common.util.concurrent RateLimiter]
           [java.util.concurrent TimeUnit]
           [java.util.concurrent Semaphore])
  )


(defn operation [desc func type]
  {:fn func
   :desc desc
   :type type})

(defn scenario
  "TODO"
  [desc & funcs]
  {:desc desc
   :ops funcs})

(defn exec
  "TODO"
  [desc func]
  (operation desc func :exec))

(defn pause
  "TODO"
  ([duration]
   (pause "Pause" duration))
  ([desc duration]
   (operation desc
              (fn pause [ctx]
                (debug "pause start")
                (Thread/sleep duration)
                (debug "pause end")
                )
              :pause))
  ([desc lower upper]
   (let [duration (+ lower (rand-int (- upper lower)))]
     (pause desc duration))
   )
  )
(comment
  (defmulti exec-op (fn fn-type [op ctx]
                      :fn))
  (defmethod exec-op :fn [op ctx]
    (let [label (str (-> ctx ::scenario :desc) " / "  (op :desc))
          func (-> op :fn)
          ]
      ;;(info label)
      ;;(swap! (-> ctx :counter-pre) inc)
      (func ctx)
      )))


(defn exec-operations [system ops scn-ctx]
  (let [op (first ops)]
    (when op
      (let [scenario-name (-> scn-ctx ::scenario :desc)
            operation-name (op :desc)
            func (-> op :fn)
            registry (-> system :metrics :registry)
            timer (timers/timer registry [scenario-name operation-name "runtime"])
            timer-ctx (timers/start timer)
            ]
        (info scenario-name " / " operation-name)
        (func scn-ctx)
        (timers/stop timer-ctx)
        (recur system (rest ops) scn-ctx)
        )
      )
    ))

(defn execute-scenario [system scenario params finish-fn]
  (info "Run scenario:" (-> scenario :desc))

  (let [scn-ctx {:params params
                 ::scenario scenario}
        ops (-> scenario :ops)
        {:keys [stats]} system]

    (info "stats" @stats)
    (try
      (swap! stats update-in [:scenarios :concurrent] inc)
      (swap! stats update-in [:scenarios :started] inc)
      (exec-operations system ops scn-ctx)
      (catch Exception e
        (error e "exec-operations failed")
        )
      (finally
       (swap! stats update-in [:scenarios :concurrent] dec)
       (swap! stats update-in [:scenarios :finished] inc)
       (finish-fn)))
    :ok
    ))

(defn- create-thread [name system scenario params finish-fn]
  (future (execute-scenario system scenario params finish-fn)))

(defn start-run [system suite finish-fn]
  (let [{:keys [scenario
                params] :as data} suite
        scn-name (-> scenario :name)]
    (create-thread scn-name system scenario params finish-fn)
    ))

(defn constant-rate-loader [system opts suite]
  (debug "Create constant-rate-loader" opts)
  (let [{:keys [rate
                scenario
                warmup-period]} opts
        {:keys [running-fn]} system
        rate-limiter (RateLimiter/create (double rate) (or warmup-period 0) TimeUnit/SECONDS)
        ]

    (while (running-fn)
      (debug "Aqcuiring slot")
      ;; TODO use timeout
      (.acquire rate-limiter)
      (debug "Acquired slot")

      (start-run system suite)
      )
    (debug "Stop constant-rate-loader")
    )
  )

(defn constant-users-loader [system opts suite]
  (debug "Create constant-users-loader" opts)
  (let [{:keys [users
                scenario
                warmup-period]} opts
        {:keys [running-fn]} system
        user-slots (new Semaphore users)
        ]
    (while (running-fn)
      ;; TODO use tryAcquire with timeout
      (.acquire user-slots)
      (start-run system suite (fn [] (.release user-slots)))
    )
  ))

(defn create-loader [system suite]
  (let [load (-> suite :load)
        type (-> load :type)]
    (info "create-loader " type load suite)
    (cond (= type :constant-rate)
          (future (constant-rate-loader system (suite :load) suite))
          (= type :constant-users)
          (future (constant-users-loader system (suite :load) suite))
          :default (throw+ {:error :unsupported-load-type
                            :msg (str type " is not supported")})
          )
    ))


(defn run-load
  "Starts load-test asynchronously."
  [system suites]
  (info "run-load start" suites)
  ;; TODO validate scenarios with schema?
  (let [loader-futures (doall (map #(create-loader system %) suites))
        ]
    (debug "loader threads created")
    loader-futures
    )
  )

