(ns grownome.routes.alerts
  (:require
   [grownome.layout :as layout]
   [grownome.db.core :as db]
   [clojure.java.io :as io]
   [clojure.string :as strings]
   [grownome.middleware :as middleware]
   [clojure.tools.logging :as log]
   [java-time :as jt]
   [ring.util.http-response :as response]))


(defn get-alerts
  [{:keys [session] :as req}]
  (let [user-id (get-in session [:identity :id])
        alerts (db/get-alerts-by-user {:user-id user-id})]
    (response/ok alerts)))

(defn get-alert
  [{:keys [session path-param] :as req}]
  (let [user-id (get-in session [:identity :id])
        alert-id (get path-param :id)
        alert (db/get-alert {:id alert-id})]
    (if alert
      (response/ok alert)
      (response/not-found))))

(defn get-alerts-admin
  []
  (let [alerts (db/get-all-alerts)]
    (response/ok alerts)))

(defn post-alert
  [{:keys [params session] :as input}]
  (let [user-id  (get-in session [:identity :id])
        params   (assoc params :user-id user-id)
        creation (db/create-alert! params)]
    (response/created "/alert/" (str (:id creation)))))

(defn post-alert-admin
  [{:keys [params session] :as input}]
  (let [creation (db/create-alert! params)]
    (response/created "/alert/" (str (:id creation)))))



(defn alerts-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats
                 middleware/wrap-restricted]}
   ["/alert"
    ["/:id"    {:get get-alert
                :post post-alert}]]
   ["/alerts"  {:get get-alerts}]
   ["/admin"
    ["/alert"  {:post post-alert-admin}]
    ["/alerts" {:get  get-alerts-admin}]]])

