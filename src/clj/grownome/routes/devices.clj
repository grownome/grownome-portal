(ns grownome.routes.devices
  (:require
   [grownome.layout :as layout]
   [grownome.db.core :as db]
   [clojure.java.io :as io]
   [clojure.string :as strings]
   [grownome.middleware :as middleware]
   [clojure.tools.logging :as log]
   [java-time :as jt]
   [ring.util.http-response :as response]))

(defn gs-to-url
  [gs-url]
  (str "https://storage.googleapis.com/" (second (strings/split gs-url #"gs:\/\/"))))

(defn prep-image
  [image]
  (assoc image :path (gs-to-url (:path image))))

(defn get-device-images
  [limit device ]
  (let [device-id (:id device)
        raw-image-data (db/get-images-by-device-limit {:id device-id :limit limit})
        url-image-data (map prep-image raw-image-data)]
    (assoc device :images url-image-data)))

(defn get-devices-admin
  [req]
  (let [devices (db/get-devices)
        devices-with-image-links (into [] (map (partial get-device-images 500) devices))]
    (response/ok devices-with-image-links)))

(defn get-devices-user
  [req]
  (let [devices (db/get-devices-by-user
                 {:id (get-in req [:session :identity :id])})
        devices-with-image-links
        (into [] (map (partial get-device-images 50) devices))]
    (response/ok devices-with-image-links)))

(defn get-devices
  [{:keys [session] :as req}]
  (if (get-in session [:identity :admin])
    (get-devices-admin req)
    (get-devices-user req)))

(defn fix-metrics-values
  [metric]
  (update metric
          (keyword (:metric-name metric))
          (fn [v] (.floatValue v))))

(defn fix-metrics-dates
  [metric]
  (update metric
          :timestamp
          (fn [v] (jt/format "MM/dd hh:mm" v))))

(def metric-fixer (comp fix-metrics-dates fix-metrics-values))

(defn get-device-metrics
  [{:keys [path-params] :as req}]
  (let [raw-metrics
        (db/get-metrics-by-device {:id (read-string  (:id path-params))})
        fixed-metrics (map metric-fixer raw-metrics)
        groups (group-by :metric-name fixed-metrics)
        resp {:id (read-string (:id path-params))
              :metrics groups}]
    (response/ok resp)))

(defn get-device
  [{:keys [path-params session] :as req}]
  (let [device-id (:id path-params)
        devices (db/get-devices-by-user
                 {:id (get-in req [:session :identity :id])})
        a-device (first (filter #(= (:id %) device-id) devices))]
    (if a-device
      (response/ok a-device)
      (response/not-found))))

(defn get-device-admin
  [{:keys [path-params session] :as req}]
  (let [device-id (:id path-params)
        device (db/get-device {:id device-id})]
    (if device
      (response/ok device)
      (response/not-found))))


(defn post-device
  [{:keys [params] :as input}]
  (let [creation (db/create-device! params)]
    (response/created "/device/" (str (:id params)))))

(defn devices-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats
                 middleware/wrap-restricted]}
   ["/devices" {:get get-devices}]
   ["/device"  {:get get-device}
    ["/:id/metrics" {:get get-device-metrics}]]
   ["/admin"
    ["/device" {:post post-device
               :get  get-device-admin}]]
   ])

