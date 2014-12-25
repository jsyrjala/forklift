(ns forklift.data
  (:require [co.paralleluniverse.pulsar.core :refer
             [spawn-fiber join suspendable! defsfn]]
            [slingshot.slingshot :refer [throw+]]
            [clojure.tools.logging :refer [info debug error]]
            )
  (:import co.paralleluniverse.strands.Strand
           co.paralleluniverse.fibers.Fiber
           com.google.common.util.concurrent.RateLimiter)

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
                (Strand/sleep duration)
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


(defn handle-run [run]
  (let [{:keys [scenario
                users
                duration
                rampup
                prefix-idle
                params] :as data} run
        scn-name (-> scenario :name)
        fiber (spawn-fiber :name (str "Fiber-" scn-name) execute-scenario scenario params)
       ]
   fiber
  ))

(defn run-load [opts & scenarios]
  (info "run-load start")
  ;; TODO validate scenarios with schema?
  (let [fibers (doall (map handle-run scenarios))]
    (debug "fibers created" )
    (join fibers)

    )
  (info "run-load end")
  )
