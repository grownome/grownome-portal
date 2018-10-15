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
 (fn [_ [device-id]]
   {:http {:method      :get
           :url         (str "/device/" device-id "/metrics")
           :error-event [:common/set-error]}})
 (fn [{:keys [db]} [_ metrics]]
   {:db
    (-> db
        (assoc-in [:focused-device ] (:id metrics))
        (assoc-in [:devices (:id metrics) :metrics]
                  (:metrics metrics)))}))

(rf/reg-sub
 :focused-device
 (fn [db _]
   (get-in db [:focused-device ])))


(defn get-metric-from-device
  [device metric-name ]
   (into [] (map (keyword metric-name) (get-in device [:metrics metric-name])))
   )

(defn get-label-from-device
  [device metric-name ]
   (into [] (map :timestamp (get-in device [:metrics metric-name]))))

(defn device-metrics-card
  [id]
  (let [device     @(rf/subscribe [:device id])
        session    @(rf/subscribe [:session])
        humidity (get-metric-from-device device "humidity")
        temp     (get-metric-from-device device "temperature")
        labels   (get-label-from-device device "humidity")]
    (fn [id]
      [b/Card
       [b/CardTitle
        [chart/chart labels humidity temp ]]
       [b/CardBody
        [:div
         [b/CardTitle (:name device)]
         (when (:admin session)
           [b/CardTitle (:resin_name device)])
         (when (:admin session)
           [b/CardTitle (:id device)])]]])))

(defn metrics-page []
  (let [device-id @(rf/subscribe [:focused-device])]
    [b/Container
     [device-metrics-card device-id]]))

