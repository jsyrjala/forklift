(ns forklift.main
  "Main function for Forklift"
  (:require [clojure.tools.logging :refer [info debug error]])
  (:gen-class)
  )

(defn -main [& args]
  (info "Forklift starting")
  (require '[forklift.core])
  (eval `(forklift.core/run))
  )
