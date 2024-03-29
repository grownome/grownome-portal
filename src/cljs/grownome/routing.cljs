(ns grownome.routing
  (:require
    [kee-frame.api :as api]
    [re-frame.core :as rf]
    [reitit.core :as reitit]
    [clojure.string :as string]
    [goog.events :as events]
    [goog.history.EventType :as HistoryEventType])
  (:import goog.History))

(def routes
  [["/" :home]
   ["/about" :about]
   ["/profile" :profile]
   ["/alerts" :alerts]
   ["/owners" :owners]
   ["/devices" :devices]
   ["/device/:id/metrics" :metrics]
   ["/user/:id" :user]])

(defonce router (reitit/router routes))

(defrecord ReititRouter [routes]
  api/Router

  (data->url [_ [route-name path-params]]
    (str "/#" ;; Remove to use urls for Browser history API
         (:path (reitit/match-by-name routes route-name path-params))
         (when-some [q (:query-string path-params)] (str "?" q))
         (when-some [h (:hash path-params)] (str "#" h))))

  (url->data [_ url]
    (let [[path+query fragment] (-> url (string/replace #"^/#" "") (string/split #"#" 2))
          [path query] (string/split path+query #"\?" 2)
          route (reitit/match-by-path routes path)]
      (rf/dispatch [:nav/route route])
      (some-> route (assoc :query-string query :hash fragment)))))

(defn match-route [uri]
  (->> (or (not-empty (string/replace uri #"^.*#" "")) "/")
       (reitit/match-by-path router)))

(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      HistoryEventType/NAVIGATE
      (fn [event]
        (rf/dispatch [:nav/route (match-route (.-token event))])))
    (.setEnabled true)))

(rf/reg-event-db
  :nav/route
  (fn [db [_ route]]
    (assoc db ::route route)))

(rf/reg-event-fx
  :nav/route-name
  (fn [_ [_ route-name]]
    (let [route (reitit/match-by-name router route-name)]
      {:navigate-to [(-> route :data :name) (:path-params route)]})))

(rf/reg-sub
  :nav/route
  (fn [db _]
    (-> db ::route)))

(rf/reg-sub
  :nav/page
  :<- [:nav/route]
  (fn [route _]
    (-> route :data :name)))
