(ns forklift.data
  (:require [co.paralleluniverse.pulsar.core :refer
             [spawn-fiber join suspendable! defsfn]]
            [slingshot.slingshot :refer [throw+]]
            [clojure.tools.logging :refer [info debug error]]
            )
  (:import co.paralleluniverse.strands.Strand
           co.paralleluniverse.fibers.Fiber
           com.google.common.util.concurrent.RateLimiter
           java.util.concurrent.TimeUnit)

)

(defn operation [desc func]
  {:fn (suspendable! func)
   :desc desc})

(defn scenario
  "TODO"
  [desc & funcs]
  {:desc desc
   :ops funcs}
  )

(defn exec
  "TODO"
  [desc func]
  (operation desc func))

(defn pause
  "TODO"
  ([duration]
   (pause "Pause" duration))
  ([desc duration]
   (operation desc
              (fn pause [ctx]
                (debug "pause start")
                (Fiber/sleep duration)
                (debug "pause end")
                )))
  ([desc lower upper]
   (let [duration (+ lower (rand-int (- upper lower)))]
     (pause desc duration))
   )
  )

(defmulti exec-op (fn fn-type [op ctx]
                    :fn))
(defmethod exec-op :fn [op ctx]
  (let [label (str (-> ctx ::scenario :desc) " / "  (op :desc))
        func (-> op :fn)
        ]
    ;;(info label)
    ;;(swap! (-> ctx :counter-pre) inc)
    (func ctx)
    ))


(defsfn exec-operation [ops ctx]
  (let [op (first ops)

        ]
    (when op
      (info (str (-> ctx ::scenario :desc) " / "  (op :desc)))

      (let [func (-> op :fn)]
        (func ctx)
        (recur (rest ops) ctx)
        )
      )
  ))

(defsfn execute-scenario [scenario params]
  (info "Run scenario:" (-> scenario :desc))

  (let [ctx {:params params
             ::scenario scenario}
        ops (-> scenario :ops)]

    (exec-operation ops ctx)

    :ok
  ))


(defn start-run [suite]
  (let [{:keys [scenario
                params] :as data} suite
        scn-name (-> scenario :name)
        fiber (spawn-fiber :name (str "Fiber-" scn-name) execute-scenario scenario params)
       ]
   fiber
  ))



(defn constant-rate-loader [opts suite running]
  (debug "constant-rate-load" opts)
  (let [{:keys [rate
                scenario
                warmup-period]} opts
        rate-limiter (RateLimiter/create (double rate) warmup-period TimeUnit/SECONDS)
        ]
    (while @running
      (.acquire rate-limiter)
      (info "Acquired slot")

      (let [fiber (start-run suite)]
        ;; TODO how to handle dangling fibers?
        )
      )
    )
  )

(defn create-loader [suite running]
  (let [{:keys [load]} suite
        type (-> load :type)]
    (cond (= type :constant-rate)
      (future (constant-rate-loader (suite :load) suite running))
      :default (throw+ {:error :unsupported-load-type
                        :msg (str type " is not supported")})
      )

    ))
(defn run-load [opts & suites]
  (info "run-load start")
  ;; TODO validate scenarios with schema?
  (let [running (atom true)
        loader-futures (doall (map #(create-loader % running) suites))
        ]
    (debug "loader threads created")

    )
  (info "run-load end")
  )
