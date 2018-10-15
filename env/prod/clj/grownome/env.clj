(ns grownome.env
  (:require [clojure.tools.logging :as log]))

(defn wrap-prod [handler]
  (fn [req]
    (-> (handler req)
        (assoc-in [:security :hsts] true)
        (assoc-in [:security :ssl-redirect] true))))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[grownome started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[grownome has shut down successfully]=-"))
   :middleware wrap-prod})

