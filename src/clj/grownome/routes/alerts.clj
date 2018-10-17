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
        alerts (db/get-alerts-by-user (db/params->snake {:user-id user-id}))]
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
  [req]
  (let [alerts (db/get-all-alerts)]
    (response/ok alerts)))

(def conditions
  {"above" #(-> %
                (assoc :above true)
                (dissoc :condition)
                (assoc :below false))
   "below" #(-> %
                (assoc :below true)
                (dissoc :condition)
                (assoc :above false))})

(defn set-condition
  [alert]
  (let [res
        ((conditions (:condition alert))
         alert)]
    res))

(defn post-alert
  [{:keys [params session] :as req}]
  (let [user-id  (get-in session [:identity :id])
        params   (assoc params :user-id user-id)
        params   (update params :device-id read-string)
        params   (update params :threshold read-string)
        ;get rid of condition value after setting the corrisponding column
        params   (set-condition params)
        creation (db/create-alert! (db/params->snake params))]
    (response/created "/alert/" (str (:id creation)))))

(defn post-alert-admin
  [{:keys [params session] :as input}]
  (let [creation (db/create-alert! (db/params->snake  params))]
    (response/created "/alert/" (str (:id creation)))))



(defn alerts-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/alerts"  {:get get-alerts
                :post post-alert}]
   ["/alert/:id" {:get get-alert}]
   ["/admin"
    ["/alerts" {:get  get-alerts-admin
                :post post-alert-admin}]]])

