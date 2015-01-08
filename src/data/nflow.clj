(ns data.nflow
  (:require [forklift.data :refer [run-load exec scenario pause]]
            [clojure.tools.logging :refer [trace info debug error]]
            [clj-http.client :as client]
            [cheshire.core :as json]
            [clj-time.format :as date]
            [clj-time.coerce :as coerce]
            [data.stats :as stats]
  ))

(def formatter (date/formatters :date-time))
;; TODO no exception handling yet -> don't throw exceptions
;; TODO no success/failure handling yet -> make sure you always succeed
;; TODO assumes that request always succeeds
;;
;; function that is tested
;; Function must take single parameter ctx
;; ctx => {:run-params somedata :global-param otherdata} - somedata is defined in suite configuration
;; Function can modify ctx for following ops by returning modified ctx
;; e.g. make a request, store some data to ctx and following ops may use the data
;;
(defn- create-workflow [ctx]
  (let [{:keys [nflow-urls
                workflow-type]} (-> ctx :params)
        workflow-ids (-> ctx :params :workflow-ids)
        body {:type workflow-type}
        result (client/put (str (rand-nth nflow-urls) "/v1/workflow-instance")
                           {:as :json
                            :accept :json
                            :content-type :json
                            :body (json/generate-string body)})]

    (let [workflow-id (-> result :body :id)]
      (swap! workflow-ids conj workflow-id))))

(defn- get-workflow [ctx workflow-id]
  (let [{:keys [nflow-urls]} (-> ctx :params)
        url (str (rand-nth nflow-urls) "/v1/workflow-instance/" workflow-id)
        result (client/get url
                           {:as :json
                            :accept :json})]
    (-> result :body)
    )
  )


(defn- parse-date [value]
  (if value
    (date/parse formatter value)
    nil))

(defn- time-diff [value1 value2]
  (- (coerce/to-long value1) (coerce/to-long value2)))

(defn- parse-workflow [workflow]
  (let [workflow (update-in workflow [:started] parse-date)
        workflow (update-in workflow [:created] parse-date)
        workflow (update-in workflow [:modified] parse-date)
        workflow (dissoc workflow :stateText)
        workflow (update-in workflow [:actions]
                            #(mapv (fn [action]
                                     (let [action (update-in action [:executionStartTime] parse-date)
                                           action (update-in action [:executionEndTime] parse-date)
                                           action (dissoc action :stateText)
                                           action (assoc action :run-time (time-diff (action :executionEndTime)
                                                                                     (action :executionStartTime) ))]
                                       action)) %))
        ;; calculate some time differences
        workflow (assoc workflow :total-time (time-diff (workflow :modified) (workflow :created)))
        workflow (assoc workflow :run-time (time-diff (workflow :modified) (workflow :started)))
        workflow (assoc workflow :queue-time (time-diff (workflow :started) (workflow :created)))]
    workflow
  ))

(defn- fetch-workflows [suite-config]
  (info "Fetch workflows from server")
  (let [ready (atom [])
        {:keys [workflow-ids
                finished-states
                wait-before-stats]} (-> suite-config :params)]
    (Thread/sleep (or wait-before-stats 1000))

    (while (not-empty @workflow-ids)
      (Thread/sleep 1000)
      (doseq [id @workflow-ids]
        (let [workflow (get-workflow suite-config id)]
          (when (some #{(:state workflow)} finished-states)
            (swap! workflow-ids disj id)
            (swap! ready conj (parse-workflow workflow))
            ))))
    (info "Fetched" (count @ready) "workflows")
    (sort-by :created @ready)))

(defn- workflow-stats [suite-config]
  (let [workflows (fetch-workflows suite-config)
        statistics (stats/workflow-stats workflows)]
    (info "statistics" statistics)
    (clojure.pprint/pprint statistics)
    )
  )

(defn seconds [value] (* value 1000))

(defn minutes [value] (* 60 (seconds value)))

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
   ;; TODO implement warmup-runs
   :warmup-runs 10
   :params {:nflow-urls ["http://localhost:7500/api"]
            :workflow-type "demo"}
   ;; configuration for loader
   :load {;; constant-users loader tries to make sure
          ;; that there is N scenarios running all the time
          :type :constant-users
          ;; real-world :users value is likely < 10
          ;; values greater than 50-100 are too heavy for single machine
          :users 10
          ;; TODO not implemented currently
          :rampup-period 60
  }})

(def basic-suite-rate
  {:scenario basic-workflow
   :desc "Constant rate"
   ;; TODO implement warmup-runs
   :warmup-runs 10
   ;; TODO needs to have global-params and run-params
   ;; global params do not reset between runs
   ;; run-params reset between runs
   :params {:workflow-type "demo"}
   :load {;; constant-rate loader starts a new scenario N times a second
          :type :constant-rate
          :rate 1
          ;; rampup period in seconds
          :rampup-period 1
  }})

;; suite config is a list of suites and ending condition
(def suite-config {;; duration of loading run in millis
                   ;; => all loaders stop after N millis
                   :duration (seconds 20)
                   :params {;; if many urls, random url is selected at
                            ;; every request
                            :nflow-urls ["http://localhost:7500/api"
                                         ;;"http://localhost:7501/api"
                                         ]
                            :workflow-ids (atom #{})
                            :finished-states ["done"]
                            :wait-before-stats (seconds 13)}
                   :suites [basic-suite-users
                            ;;basic-suite-rate
                            ]
                   ;; execute function before starting
                   :before-fn (fn [suite-config]
                                (info "before-fn called"))
                   ;; execute after finishing
                   :after-fn workflow-stats})
