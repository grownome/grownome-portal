(ns grownome.routes.oauth
  (:require [ring.util.http-response :refer [ok found]]
            [clojure.java.io :as io]
            [grownome.db.core :as  db]
            [clj-http.client :as client]
            [ring.middleware.oauth2 :as oauth2]
            [grownome.oauth2 :as okta]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [camel-snake-kebab.core :as kebab]
            [clojure.tools.logging :as log]
            [ring.util.http-response :as response]))

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

(defn get-or-create-user
  [auth0-identity]
  (log/debug auth0-identity)
  (if-let [user (db/get-user {:id (:sub auth0-identity)})]
    (do
      (db/update-user-last-used!
       (db/params->snake
        {:last-login (new java.util.Date)
         :is-active true
         :id (:sub auth0-identity)}))
      user)
    (do
      (let [is-admin (if (re-find #".*@grownome.com$"
                                  (:email auth0-identity))
                       true
                       false)]
        (db/create-user! {:email (:email auth0-identity)
                          :id (:sub auth0-identity)})
        (if is-admin
          (db/update-user-is-admin! {:admin true
                                     :id (:sub auth0-identity)})
          (db/update-user-is-admin! {:admin false
                                     :id (:sub auth0-identity)}))
        (db/update-user-last-used!
         (db/params->snake
          {:last-login (new java.util.Date)
           :is-active true
           :id (:sub auth0-identity)})))
      (db/get-user {:id (:sub auth0-identity)}))))

(defn get-user-profile-wrap
  [handler]
  (fn
    [req]
    (let [{:keys [session] :as resp} (handler req)
          auth0-identity (get-oauth-metadata (access-token-from-session session))
          db-entry (get-or-create-user auth0-identity)]
      (-> resp
          (assoc-in [:session ::oauth2/profile]
                    auth0-identity)
          (assoc-in [:session :identity]
                    db-entry)))))

(defn sign-out
  [req]
  (response/permanent-redirect "https://grownome.auth0.com/v2/logout"))

(defn drop-session
  [handler]
  (fn [req]
    (let [resp (handler req)]
      (assoc resp :session nil))))

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
    (let [{:keys [user-id screen-name]} callback]
      (-> (found "/")
          (assoc :session
                 (assoc session :user-id user-id :screen-name screen-name)))))
  callback)


(defn oauth-routes []
  ["/auth"
   ["/init" {:get (okta/wrap-oauth2-okta oauth-init)}]
   ["/out" {:get (drop-session sign-out)}]
   ["/logout" {:get (drop-session #(response/permanent-redirect "/"))}]
   ["/callback" {:get (get-user-profile-wrap (okta/wrap-oauth2-okta  oauth-callback))}]])

