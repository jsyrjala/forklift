(ns forklift.core
  (:require [forklift.data :refer [run-load exec scenario pause]]
            [clojure.tools.logging :refer [info debug error]]
            [forklift.system :as system]
  ))


(defn run [& args]
  (println "started")


  (let [func1 (fn [ctx] (info "exec func1") )
        func2 (fn [ctx] (info "exec func2") 2)
        func3 (fn [ctx] (info "exec func3") 3)
        s (scenario "Basic load"
                 (exec "Operation 1"
                      func1)
                 ;;(pause 10)
                 (exec "Operation 2"
                       func2)
                 (pause 200)
                 (exec "Oper 3" func3))]

    (run-load {}
              {:scenario s :desc "run1" :params {:a 1}
               :load {:type :constant-rate
                      ;; number of new users per second
                      :rate 6
                      ;; rampup period in seconds
                      :warmup-period 15}}
              {:scenario s :desc "run2" :params {:a 1}
               :load {:type :constant-rate
                      ;; number of new users per second
                      :rate 7.1
                      ;; rampup period in seconds
                      :warmup-period 30}}
              )
    )
  )
