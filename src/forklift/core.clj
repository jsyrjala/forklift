(ns forklift.core
  (:require [co.paralleluniverse.pulsar.core :refer [spawn-fiber join]] )
  )

(defn run [& args]
  (println "started")


  (let [fib (spawn-fiber (fn [] (println "fiber")))]
    (join fib)
    )
  )
