(ns grownome.handler
  (:require [grownome.middleware :as middleware]
            [grownome.layout :refer [error-page]]
            [grownome.routes.home :refer [home-routes]]
            [grownome.routes.devices :refer [devices-routes]]
            [grownome.routes.alerts :refer [alerts-routes]]
            [grownome.routes.oauth :refer [oauth-routes]]
            [reitit.ring :as ring]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.webjars :refer [wrap-webjars]]
            [grownome.env :refer [defaults]]
            [mount.core :as mount]))

(mount/defstate init-app
  :start ((or (:init defaults) identity))
  :stop  ((or (:stop defaults) identity)))


(mount/defstate app
  :start
  (middleware/wrap-base
    (ring/ring-handler
      (ring/router
        [(home-routes)
         (devices-routes)
         (alerts-routes)
         (oauth-routes)])
      (ring/routes
        (ring/create-resource-handler
          {:path "/"})
        (wrap-content-type
          (wrap-webjars (constantly nil)))
        (ring/create-default-handler
          {:not-found
           (constantly (error-page {:status 404, :title "404 - Page not found"}))
           :method-not-allowed
           (constantly (error-page {:status 405, :title "405 - Not allowed"}))
           :not-acceptable
           (constantly (error-page {:status 406, :title "406 - Not acceptable"}))})))))
