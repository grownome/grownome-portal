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

;i'm sorry
(def make-pred
  {[true false] >
   [false true] <})

(defn build-one-checker
  [{:keys [device-id
           created-on
           user-id
           metric-name
           threshold
           above
           below
           phone-number
           description] :as alert}]
  (fn [metric]
    (if-let [value (get metric (keyword metric-name))]
      (if ((make-pred [above below]) metric threshold)
        [{:alert alert
           :metric metric}]
        [])
      []
      )))

(defn build-checker
  [alerts]
  (fn [metric]
    (reduce
     (fn [accum one-checker]
       (concat accum (one-checker metric)))
     []
     (map build-one-checker alerts))))

(defn check-metric-for-alerts
  [alerts metrics]
  (let [checker (build-checker alerts)]
    (map checker metrics)))

(defn check-alerts-for-device-id
  [alerts metrics]
  (let [alerts-by-metric
        (group-by :metric_name alerts)
        metrics-by-name
        (group-by :metric_name metrics)
        alert-metric-names (into #{} (keys alerts-by-metric))
        metric-metric-names (into #{} (keys metrics-by-name))
        shared-metric-names (clojure.set/intersection
                             alert-metric-names
                             metric-metric-names)]
    (pmap
     (fn [metric-name]
       (check-metric-for-alerts
        (get alerts-by-metric metric-name)
        (get metric-metric-names metric-name)))
     shared-metric-names)))

(defn message-number
  [phone]
  "wdmrZiZRrVUwpDe81zHZThpg")

(defn fire-alerts
  [{:keys [alert metric]}])

(defn check-alerts
  [{:keys [params headers] :as input}]
  (when (get headers "X-Appengine-Cron"))
  (let [alerts (db/get-all-alerts)
        alerts-rules-by-device (group-by :device-id)
        alert-device-ids   (keys alerts-rules-by-device)
        last-5-minutes (db/get-metrics-summary {:interval 60 :limit 300})
        metrics-by-device (group-by :device-id last-5-minutes)
        metrics-device-ids (keys metrics-by-device)
        shared-device-ids (clojure.set/intersection
                           alert-device-ids
                           metrics-device-ids)]
    (pmap
     (fn [device-id]
       (check-alerts-for-device-id
        (get alerts-rules-by-device device-id)
        (get metrics-by-device device-id))))))


(defn alerts-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/alerts"  {:get get-alerts
                :post post-alert}]
   ["/check-alerts" {:post check-alerts}]
   ["/alert/:id" {:get get-alert}]
   ["/admin"
    ["/alerts" {:get  get-alerts-admin
                :post post-alert-admin}]]])

