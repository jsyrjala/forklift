(ns data.nflow
  (:require [forklift.data :refer [run-load exec scenario pause]]
            [clojure.tools.logging :refer [trace info debug error]]
            [clj-http.client :as client]
            [cheshire.core :as json]
  ))


;; TODO no exception handling yet -> don't throw exceptions
;; TODO no success/failure handling yet -> make sure you always succeed
;; TODO assumes that request always succeeds
;;
;; function that is tested
;; Function must take single parameter ctx
;; ctx => {:params somedata} - somedata is defined in suite configuration
;; Function can modify ctx for following ops by returning modified ctx
;; e.g. make a request, store some data to ctx and following ops may use the data
;;
(defn create-workflow [ctx]
  (trace ctx)
  (let [{:keys [workflow-ids
                nflow-url
                workflow-type]} (-> ctx :params)
        body {:type workflow-type}
        result (client/put (str nflow-url "/v1/workflow-instance")
                                {:accept :json
                                 :content-type :json
                                 :as :json
                                 :body (json/generate-string body)})]

    (let [workflow-id (-> result :body :id)
          x (assoc ctx (str "xxx-" workflow-id) workflow-id)]
      (swap! workflow-ids conj workflow-id))))

;; scenario is one use case, test case or a sequence operations
;; executed by a single user
;;
;; 1. create new workflow
;; 2. wait a bit
(def basic-workflow
  (scenario
   ;; human readable description for scenarion
   "Basic workflow"
   ;; 0-N operations that are executed sequentially
   ;; exec marks executable operation
   (exec
    ;; description of the operation
    "Create a new Workflow"
    ;; function that is executed
    create-workflow)
   ;; pause of 10 millis
   (pause 10)))

;; suite parametrizes scenario with
;; - loader that controls how often and when the scenario is executed
;; - configuration data, e.g. what server to contact
(def basic-suite-users
  {:scenario basic-workflow
   :desc "Constant users"
   :params {:nflow-url "http://localhost:7500/api"
            :workflow-type "demo"
            :workflow-ids (atom [])}
   ;; configuration for loader
   :load {;; constant-users loader tries to make sure
          ;; that there is N scenarios running all the time
          :type :constant-users
          ;; real-world :users value is likely < 10
          ;; values greater than 50-100 are too heavy for single machine
          :users 1
          ;; TODO not implemented currently
          :warmup-period 60
  }})

(def basic-suite-rate
  {:scenario basic-workflow
   :desc "Constant rate"
   ;; TODO needs to have global-params and run-params
   ;; global params do not reset between runs
   ;; run-params reset between runs
   :params {:nflow-url "http://localhost:7500/api"
            :workflow-type "demo"
            :workflow-ids (atom [])}
   :load {;; constant-rate loader starts a new scenario N times a second
          :type :constant-rate
          :rate 1
          ;; rampup period in seconds
          :warmup-period 1
  }})

;; suite config is a list of suites and ending condition
(def suite-config {;; duration of loading run in millis
                   ;; => all loaders stop after N millis
                   :duration (* 10 1000)
                   :suites [;;basic-suite-users
                            basic-suite-rate]})
