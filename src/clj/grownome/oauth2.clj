(ns grownome.oauth2
  (:require
   [grownome.config :as c]
   [ring.middleware.oauth2 :refer [wrap-oauth2]]))

(defn wrap-oauth2-okta
  [handler]
  (wrap-oauth2
   handler
   {:auth0
    {:authorize-uri    "https://grownome.auth0.com/authorize"
     :access-token-uri "https://grownome.auth0.com/oauth/token"
     :client-id        "0CEOIFVgNm8c734pDybJHGW3y7bIRhHF"
     :client-secret    "-EqyPoMjgl1GnI-ZNEPn8GitipPJKdY7V4jXGg7y5fcFssenP_mIH-kmbd7iOemq"
     :scopes           [:profile :email :openid]
     :launch-uri       (or (-> c/env :oauth-launch-uri)  "/auth/init")
     :redirect-uri     (or (-> c/env :oauth-redirect-uri) "/auth/callback")
     :logout-uri       (or (-> c/env :oauth-logout-uri) "/auth/logout")
     :landing-uri      (or (-> c/env :oauth-landing-uri ) "/")}}))

