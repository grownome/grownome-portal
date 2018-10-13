(ns grownome.routes.home
  (:require [grownome.layout :as layout]
            [grownome.db.core :as db]
            [clojure.java.io :as io]
            [grownome.middleware :as middleware]
            [clojure.tools.logging :as log]
            [ring.util.http-response :as response]))

(defn home-page [request]
  (layout/render "home.html"))

(defn profile [request]
  (response/ok (get-in request [:session :identity]) ))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/profile" {:get profile}]])

