(ns grownome.ajax
  (:require [ajax.core :as ajax]
            [luminus-transit.time :as time]
            [cognitect.transit :as transit]
            [re-frame.core :as rf]))

(defn local-uri? [{:keys [uri]}]
  (not (re-find #"^\w+?://" uri)))

(defn default-headers [request]
  (if (local-uri? request)
    (-> request
        (update :headers #(merge {"x-csrf-token" js/csrfToken} %)))
    request))

;; injects transit serialization config into request options
(defn as-transit [opts]
  (merge {:raw             false
          :format          :transit
          :response-format :transit
          :reader          (transit/reader :json time/time-deserialization-handlers)
          :writer          (transit/writer :json time/time-serialization-handlers)}
         opts))

(defn load-interceptors! []
  (swap! ajax/default-interceptors
         conj
         (ajax/to-interceptor {:name "default headers"
                               :request default-headers})))

(def http-methods
  {:get    ajax/GET
   :post   ajax/POST
   :put    ajax/PUT
   :delete ajax/DELETE})

(rf/reg-fx
  :http
  (fn [{:keys [method
               url
               success-event
               error-event
               params
               ajax-map]
        :or   {error-event [:common/set-error]
               ajax-map    {}}}]
    ((http-methods method)
      url (merge
            {:params        params
             :handler       (fn [response]
                              (when success-event
                                (rf/dispatch (conj success-event response))))
             :error-handler (fn [error]
                              (rf/dispatch (conj error-event error)))}
            ajax-map))))


(def ajax-chain
  {;; Is the effect in the map?
   :effect-present? (fn [effects] (:http effects))
   ;; The dispatch set for this effect in the map returned from the event handler
   :get-dispatch    (fn [effects]
                      (get-in effects [:http :success-event]))
   ;; Framework will call this function to insert inferred dispatch to next handler in chain
   :set-dispatch    (fn [effects dispatch]
                      (assoc-in effects [:http :success-event] dispatch))})


