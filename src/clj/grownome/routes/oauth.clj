(ns grownome.routes.oauth
  (:require [ring.util.http-response :refer [ok found]]
            [clojure.java.io :as io]
            [clj-http.client :as client]
            [ring.middleware.oauth2 :as oauth2]
            [grownome.oauth2 :as okta]
            [clojure.tools.logging :as log]))

(defn oauth-init
  "Initiates the 0auth"
  [request]
  (log/debug "in init")
  request)

(defn access-token-from-session
 [session]
  (get-in session [::oauth2/access-tokens :auth0 :token]))

(defn get-oauth-metadata
  [access-token]
  (:body
   (client/get "https://grownome.auth0.com/userinfo"
               {:headers {"Authorization"
                          (str "Bearer " access-token)}
                :as :json})))

(defn get-user-profile-wrap
  [handler]
  (fn
    [req]
    (let [{:keys [session] :as resp} (handler req)
          auth0-identity (get-oauth-metadata (access-token-from-session session))
          email  (:email auth0-identity)
          is-admin (if (re-find #".*@grownome.com$" email ) true false )]
      (-> resp
          (assoc-in [:session ::oauth2/profile]
                    auth0-identity)
          (assoc-in [:session :identity]
                    {:email (:email auth0-identity)
                     :admin? is-admin})))))

(defn oauth-callback
  "Handles the callback from 0auth."
  [{:keys [session params] :as callback}]
  ; oauth request was denied by user
  (log/debug "testing")
  (log/debug callback)
  (if (:denied params)
    (-> (found "/")
        (assoc :flash {:denied true}))
    ; fetch the request token and do anything else you wanna do if not denied.
    (let [{:keys [user_id screen_name]} callback]

      (-> (found "/")
          (assoc :session
                 (assoc session :user-id user_id :screen-name screen_name)))))
  callback

  )


(defn oauth-routes []
  ["/auth"
   ["/init" {:get (okta/wrap-oauth2-okta oauth-init)}]
   ["/callback" {:get (get-user-profile-wrap (okta/wrap-oauth2-okta  oauth-callback))}]])

