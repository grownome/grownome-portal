(ns grownome.routes.devices
  (:require
   [grownome.layout :as layout]
   [grownome.db.core :as db]
   [clojure.java.io :as io]
   [clojure.string :as strings]
   [grownome.middleware :as middleware]
   [clojure.tools.logging :as log]
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

(defn get-devices
  [req]
  (let [devices (db/get-devices)
        devices-with-image-links (into [] (map (partial get-device-images 50) devices))]
    (response/ok devices-with-image-links)))

(defn post-device
  [{:keys [params] :as input}]
  (log/debug params)
  (let [creation (db/create-device! params)]
    (response/created "/device/" (str (:id params)))))

(defn devices-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/devices" {:get get-devices}]
   ["/device" {:post post-device}]
   ])

