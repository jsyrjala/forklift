(ns data.nflow
  (:require [forklift.data :refer [run-load exec scenario pause]]
            [clojure.tools.logging :refer [info debug error]]
            [clj-http.client :as client]
            [cheshire.core :as json]
  ))


(defn create-workflow [ctx]
  ;;(info "create-workflow" ctx)
  (let [{:keys [workflow-ids
                nflow-url
                workflow-type]} (-> ctx :params)
        body {:type workflow-type}
        result (client/put (str nflow-url "/v1/workflow-instance")
                                {:accept :json
                                 :content-type :json
                                 :body (json/generate-string body)})]
    (info "result" result)
    ctx
  ))

(def basic-workflow
  (scenario "Basic load"
            (exec "Create new BasicWorkflow"
                  create-workflow)
            (pause 10)))

(def basic-suite
  {:scenario basic-workflow
   :desc ""
   :params {:nflow-url "http://localhost:7500/api"
            :workflow-type "demo"
            :workflow-ids (atom [])}
   :load {:type :constant-users
          ;; real-world :users value is < 10
          ;; values greater than 50-100 are too heavy for single machine
          :users 10
          :warmup-period 60
  }})

(def basic-suite2
  {:scenario basic-workflow
   :desc ""
   :params {:nflow-url "http://localhost:7500/api"
            :workflow-type "demo"
            :workflow-ids (atom [])}
   :load {:type :constant-rate
          :rate 1
  }})
