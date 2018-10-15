(ns grownome.middleware
  (:require [grownome.env :refer [defaults]]
            [cheshire.generate :as cheshire]
            [cognitect.transit :as transit]
            [clojure.tools.logging :as log]
            [grownome.oauth2 :refer [wrap-oauth2-okta]]
            [grownome.layout :refer [error-page]]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [grownome.middleware.formats :as formats]
            [muuntaja.middleware :refer [wrap-format wrap-params]]
            [buddy.auth.accessrules :refer [wrap-access-rules]]
            [grownome.config :refer [env]]
            [ring.middleware.flash :refer [wrap-flash]]
            [immutant.web.middleware :refer [wrap-session]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [buddy.auth.accessrules :refer [restrict]]
            [buddy.auth :refer [authenticated?]]
            [buddy.auth.backends.session :refer [session-backend]])
  (:import
           [org.joda.time ReadableInstant]))

(defn wrap-internal-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (log/error t (.getMessage t))
        (error-page {:status 500
                     :title "Something very bad has happened!"
                     :message "We've dispatched a team of highly trained gnomes to take care of the problem."})))))

(defn wrap-csrf [handler]
  (wrap-anti-forgery
    handler
    {:error-response
     (error-page
       {:status 403
        :title "Invalid anti-forgery token"})}))


(defn wrap-formats [handler]
  (let [wrapped (-> handler wrap-params (wrap-format formats/instance))]
    (fn [request]
      ;; disable wrap-formats for websockets
      ;; since they're not compatible with this middleware
      ((if (:websocket? request) handler wrapped) request))))

(defn admin?
  [{:keys [session] :as req}]
  (get-in session [:identity :admin]))

(def rules
  [{:pattern #"^/devices"
    :handler authenticated?}
   {:pattern #"^/admin/.*"
    :handler {:and [authenticated? admin?]}}])

(defn on-error [request response]
  (error-page
    {:status 403
     :title (str "Access to " (:uri request) " is not authorized")}))

(defn wrap-restricted [handler]
  (restrict handler {:handler authenticated?
                     :on-error on-error}))


(defn wrap-auth [handler]
  (let [backend (session-backend)]
    (-> handler
        (wrap-access-rules {:rules rules :on-error on-error})
        (wrap-authentication backend)
        (wrap-authorization backend))))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      wrap-auth
      wrap-flash
      (wrap-session {:cookie-attrs {:http-only false}})
      (wrap-defaults
        (-> site-defaults
            (assoc-in [:security :anti-forgery] true)
            (dissoc :session)))
      wrap-internal-error))

