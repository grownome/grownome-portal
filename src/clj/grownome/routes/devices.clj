(ns grownome.routes.devices
  (:require
   [grownome.layout :as layout]
   [grownome.db.core :as db]
   [clojure.java.io :as io]
   [grownome.middleware :as middleware]
   [clojure.tools.logging :as log]
   [ring.util.http-response :as response]))

(defn get-devices
  [req]
  (response/ok (db/get-devices)))

(defn post-device
  [{:keys [body] :as input}]
  (log/debug input)
  (let [creation (db/create-device! body)]
    (response/created creation)))

(defn devices-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/devices" {:get get-devices}]
   ["/device" {:post post-device}]
   ])

