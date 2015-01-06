(ns forklift.core
  (:require [forklift.data :refer [run-load exec scenario pause]]
            [clojure.tools.logging :refer [info debug error]]
            [com.stuartsierra.component :as component]
            [forklift.system :as system]
            [forklift.load-tester :as lt]
            [data.nflow :as nflow]
  ))

(def system)

(defn run [& args]
  (println "started")

  (let [app (component/start (system/create-dev-system))]
    (alter-var-root #'system (constantly app))
    )

  (let [suite-config {:duration (* 10 1000)
                      :suites [nflow/basic-suite2]}
        load-tester (:load-tester system)
        loaders (lt/start-load-test load-tester suite-config)
        ]

    (doall (map deref loaders)))

  (info "run end")
  (shutdown-agents)
  )
