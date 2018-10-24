(ns grownome.routes.owners
  (:require
   [grownome.layout :as layout]
   [grownome.db.core :as db]
   [clojure.java.io :as io]
   [clojure.string :as strings]
   [grownome.middleware :as middleware]
   [clojure.tools.logging :as log]
   [java-time :as jt]
   [ring.util.http-response :as response])
  )

(defn post-owner-admin
  [{:keys [params session] :as req}]
  (let [email (:email params)
        device-id (:device-id params)
        user (db/get-user-by-email {:email email})
        device (db/get-device {:id device-id})]
    (if (and device user)
      (let [db-req (db/add-owner!
                    {:device_id device-id
                     :user_id (:id user)
                     :created_on (java.util.Date.)})]
        (if (= 1 db-req)
          (response/ok)
          (response/internal-server-error)))
      (response/not-found {:device device :user user}))))

(defn owners-admin
  [_]
  (response/ok (db/get-owners)))

(defn owner-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/admin"
    ["/owners" {:post post-owner-admin
                :get owners-admin

                }]]])

