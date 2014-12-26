(ns forklift.data-test
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [forklift.data :refer :all]))

(defn func1 [ctx])
(defn func2 [ctx])

(fact (scenario "Scenario name"
                (exec "Operation 1"
                      func1)
                (exec "Operation 2"
                      func2))
      =>
      {:desc "Scenario name"
       :ops [{:desc "Operation 1"
              :fn func1
              :type :exec}
             {:desc "Operation 2"
              :fn func2
              :type :exec}]})



