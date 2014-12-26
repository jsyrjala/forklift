 (ns user
   (:require [clojure.java.io :as io]
             [clojure.string :as str]
             [clojure.pprint :refer (pprint)]
             [clojure.repl :refer :all]
             [clojure.test :as test]
             [clojure.tools.namespace.repl :refer (refresh refresh-all)]
             [clojure.tools.logging :refer (trace debug info warn error)]
             [forklift.system :as system]
             [com.stuartsierra.component :as component]))

 (def system nil)

(defn init
  "Constructs the current development system."
  []
  (alter-var-root #'system
                  (constantly (system/create-dev-system))))

(defn start
  "Starts the current development system."
  []
  (try
    (alter-var-root #'system component/start)
    (catch Exception e (error "Failed to start system" e)
      (throw e))))

(defn stop
  "Shuts down and destroys the current development system."
  []
  (try
    (alter-var-root #'system
                    (fn [s] (when s (component/stop s))))
    (catch Exception e (error "Failed to stop system" e)
      (throw e))))


(defn go
  "Initializes the current development system and starts it running."
  []
  (init)
  (start))

(defn reset []
  (stop)
  (info "Resetting...")
  (refresh :after 'user/go)
  (info "Reset complete")
  :reset-complete)
