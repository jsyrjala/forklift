(ns forklift.data
  (:require [co.paralleluniverse.pulsar.core :refer
             [spawn-fiber join suspendable! defsfn]]
            [slingshot.slingshot :refer [try+ throw+]]
            [clojure.tools.logging :refer [trace debug info error]]
            [metrics.meters :as meters]
            [metrics.timers :as timers]
            )
  (:import co.paralleluniverse.strands.Strand
           co.paralleluniverse.fibers.Fiber
           com.google.common.util.concurrent.RateLimiter
           java.util.concurrent.TimeUnit)
  )



(defn operation [desc func type]
  {:fn (suspendable! func)
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
                (Strand/sleep duration)
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


(defsfn exec-operation [system ops scn-ctx]
  (let [op (first ops)

        ]
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

(defsfn execute-scenario [system scenario params]
  (info "Run scenario:" (-> scenario :desc))

  (let [scn-ctx {:params params
                 ::scenario scenario}
        ops (-> scenario :ops)]

    (exec-operation system ops scn-ctx)

    :ok
    ))


(defn start-run [system suite]
  (let [{:keys [scenario
                params] :as data} suite
        scn-name (-> scenario :name)
        fiber (spawn-fiber :name (str "Fiber-" scn-name) execute-scenario system scenario params)
        ]
    fiber
    ))

;; TODO not good?
(defn- clean-fibers [fibers]
  (doseq [fiber @fibers]
    (try+
     (join 1 :ns fiber)
     (swap! fibers disj fiber)
     (catch java.util.concurrent.TimeoutException _
       ;; ignore
       ))
    )
  )

(defn constant-rate-loader [system opts suite]
  (debug "Create constant-rate-loader" opts)
  (let [{:keys [rate
                scenario
                warmup-period]} opts
        {:keys [running]} system
        rate-limiter (RateLimiter/create (double rate) warmup-period TimeUnit/SECONDS)
        ;;fibers (atom #{})
        ]

    (while @running
      (debug "Aqcuiring slot")
      (.acquire rate-limiter)
      (debug "Acquired slot")

      (let [fiber (start-run system suite)]
        ;; TODO how to handle dangling fibers?
        ;;(swap! fibers conj fiber)
        ;;(info (count @fibers) " fibers created")
        ;;(clean-fibers fibers)
        )
      )
    (debug "Stop constant-rate-loader")
    ;;(info "Clean up fibers")
    ;;(join @fibers)
    ;;(info "Cleaned")
    )

  )

(defn create-loader [system suite]
  (info "create-loader"  )
  (let [load (-> suite :load)
        type (-> load :type)]
    (cond (= type :constant-rate)
          (future (constant-rate-loader system (suite :load) suite))
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

