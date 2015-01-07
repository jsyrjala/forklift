(ns data.stats
  (:require [incanter.core :as incanter]
            [incanter.stats :refer [mean] :as stats]
            [incanter.charts :as charts]
            [clojure.tools.logging :refer [trace info debug error]]
            [flatland.ordered.map :refer [ordered-map]]
            )
  )


(defn calculate-stats [data]
  (let [c (count data)
        m (stats/mean data)
        stdev (stats/sd data)
        q (stats/quantile data :probs [0.0 0.25 0.5 0.75 0.95 0.99 1.0])]
    (ordered-map :count c
                 :mean m
                 :stdev stdev
                 :min (nth q 0)
                 :p25 (nth q 1)
                 :p50 (nth q 2) ;; = median
                 :p75 (nth q 3)
                 :p95 (nth q 4)
                 :p99 (nth q 5)
                 :max (nth q 6))
    ))

(defn batch-duration
  "Difference between first created and latest modified"
  [workflows]
  (let [created (:created (first (sort-by :created workflows)))
        modified (:modified (last (sort-by :modified workflows)))]
    (- (clj-time.coerce/to-long modified)
       (clj-time.coerce/to-long created))
  ))


(defn- action-stats [workflows]
  (let [type-actions (map :actions workflows)
        all-actions (flatten type-actions)
        grouped-actions (group-by :state all-actions)
        data (map (fn [[state actions]]
                    (let [run-times (map :run-time actions)
                          retries (map :retryNo actions)]
                      {state {:count (count actions)
                              :run-time (calculate-stats run-times)
                              :retries (calculate-stats retries)
                              }})
                    ) grouped-actions)]
    (apply merge data)
  ))

(defn- workflow-time-stats [workflows]
  (let [total-times (map :total-time workflows)
        run-times (map :run-time workflows)
        queue-times (map :queue-time workflows)]
    (ordered-map :batch-time (batch-duration workflows)
                 :count (count workflows)
                 :total-time (calculate-stats total-times)
                 :run-time (calculate-stats run-times)
                 :queue-time (calculate-stats queue-times))
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
        all-stats (ordered-map :all-workflows (workflow-time-stats workflows))
        types (group-by :type workflows)
        type-stats (map (fn [[type flows]]
                          {type
                           (assoc (workflow-time-stats flows)
                             :actions (action-stats flows))}
                          ) types)
        ]
    (assoc all-stats :types (apply merge type-stats))
  ))
