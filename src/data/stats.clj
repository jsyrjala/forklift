(ns data.stats
  (:require [incanter.core :as incanter]
            [incanter.stats :refer [mean] :as stats]
            [incanter.charts :as charts]
            [clojure.tools.logging :refer [trace info debug error]]
            )
  )


(defn calculate-stats [data]
  (let [c (count data)
        m (stats/mean data)
        stdev (stats/sd data)
        q (stats/quantile data :probs [0.0 0.25 0.5 0.75 0.95 0.99 1.0])]
    {:count c
     :mean m
     :stdev stdev
     :min (nth q 0)
     :p25 (nth q 1)
     :p50 (nth q 2) ;; = median
     :p75 (nth q 3)
     :p95 (nth q 4)
     :p99 (nth q 5)
     :max (nth q 6)}
  ))

(defn batch-duration
  "Difference between first created and latest modified"
  [workflows]
  (let [created (:created (first (sort-by :created workflows)))
        modified (:modified (last (sort-by :modified workflows)))]
    (- (clj-time.coerce/to-long modified)
       (clj-time.coerce/to-long created))
  ))

(defn- workflow-time-stats [workflows]
  (let [total-times (map :total-time workflows)
        run-times (map :run-time workflows)
        queue-times (map :queue-time workflows)
        ]
    {:batch-time (batch-duration workflows)
     :count (count workflows)
     :total-time (calculate-stats total-times)
     :run-time (calculate-stats run-times)
     :queue-time (calculate-stats queue-times)
     }
  ))

(defn workflow-types [workflows]
  (vec (reduce (fn [acc workflow] (conj acc (:type workflow)))
               #{} workflows)
  ))

(defn workflow-stats [workflows]
  (let [types (workflow-types workflows)
        total-times (map :total-time workflows)
        run-times (map :run-time workflows)
        queue-times (map :queue-time workflows)
        all-stats {:all-workflows (workflow-time-stats workflows)}
        types (group-by :type workflows)
        type-stats (map (fn [[k v]]
                          {k (workflow-time-stats v)}
                          ) types)
        ]
    (assoc all-stats :types (apply merge type-stats))
  ))

{;; workflow-type
 :demo {
        :count 42
        :run-time {:mean 0
                   :stdev 0
                   :min 0
                   :p25 0
                   :p50 0
                   :p75 0
                   :max 0}
        :queue-time {}
        :total-time {}
        :states {:done {
                        :count 10
                        ;; fetch latest from each seq (or just stats)
                        :retries {}
                        :runtime {}
                        }
                 :error {}
                 }
        }}



