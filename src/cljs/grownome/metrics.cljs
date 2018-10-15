(ns grownome.metrics
  (:require [baking-soda.core :as b]
            [kee-frame.core :as kf]
            [markdown.core :refer [md->html]]
            [reagent.core :as r]
            [cognitect.transit :as t]
            [re-frame.core :as rf]
            [grownome.chart :as chart]
            [grownome.ajax :as ajax]
            [grownome.routing :as routing]))

(kf/reg-controller
 ::device-metrics-controller
 {:params (fn [{:keys [data path-params]}]
            (when  (= :metrics (:name data))
              (:id path-params)))
  :start (fn [ctx id]
           [::load-device-metrics-page id])})

(kf/reg-chain
 ::load-device-metrics-page
 (fn [{:keys [db]} [device-id]]
   {:db (assoc db :loading-metrics true)
    :http {:method      :get
           :url         (str "/device/" device-id "/metrics")
           :error-event [:common/set-error]}})
 (fn [{:keys [db]} [_ metrics]]
   {:db
    (-> db
        (assoc-in [:loading-metrics] false)
        (assoc-in [:focused-device ] (:id metrics))
        (assoc-in [:devices (:id metrics) :metrics]
                  (:metrics metrics)))}))

(rf/reg-sub
 :focused-device
 (fn [db _]
   (get-in db [:focused-device ])))

(rf/reg-sub
 :loading-metrics
 (fn [db _]
   (get-in db [:loading-metrics])))



(defn get-metric-from-device
  [device metric-name ]
   (into [] (map (keyword metric-name) (get-in device [:metrics metric-name])))
   )

(defn get-label-from-device
  [device metric-name ]
   (into [] (map :timestamp (get-in device [:metrics metric-name]))))

(defn device-metrics-card
  [id metric-name]
  (let [device     @(rf/subscribe [:device id])
        session    @(rf/subscribe [:session])
        metrics    (get-metric-from-device device metric-name)
        labels     (get-label-from-device device metric-name)]
    (fn [id]
      [b/Card
       [b/CardTitle
        [chart/chart labels metric-name metrics]]
       [b/CardBody
        [:div
         [b/CardTitle (:name device)]
         (when (:admin session)
           [b/CardTitle (:resin_name device)])
         (when (:admin session)
           [b/CardTitle (:id device)])]]])))

(defn metrics-page []
  (let [device-id @(rf/subscribe [:focused-device])
        loading   @(rf/subscribe [:loading-metrics])
        session   @(rf/subscribe [:session])
        ]
    (when (and (not loading) (:email session) )
      [b/Container
       [device-metrics-card device-id "humidity"]
       [device-metrics-card device-id "temperature"]
       ])))

