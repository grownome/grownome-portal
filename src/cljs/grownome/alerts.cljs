(ns grownome.alerts
  (:require [baking-soda.core :as b]
            [kee-frame.core :as kf]
            [markdown.core :refer [md->html]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [grownome.devices :as devices]
            [grownome.ajax :as ajax]
            [grownome.routing :as routing]))


(kf/reg-controller
 ::alerts-controller
 {:params (fn [{:keys [data path-params]}]
            (when (= :alerts (:name data)) true))
  :start  [::load-alerts-page
           :grownome.devices/load-devices-page]})

(kf/reg-chain
 ::load-alerts-page
 (fn [_ _]
   {:http {:method      :get
           :url         "/alerts"
           :error-event [:common/set-error]}})
 (fn [{:keys [db]} [_ alerts]]
   {:db (assoc db :alerts (into {} (map #(vector (:id %) %) alerts)) )}))

(kf/reg-chain
 ::post-alert
 (fn [_ [ alert]]
   {:http {:method      :post
           :url         "/alert"
           :ajax-map {:params alert}
           :error-event [:common/set-error]}})
 (fn [{:keys [db]} [_ alert]]
   {:dispatch [::load-alerts-page]}))


(rf/reg-sub
 :alert-ids
 (fn [db _]
   (keys (:alerts db))))

(rf/reg-sub
 :alert
 (fn [db [_ id]]
   (get-in db [:alerts id])))

(defn alert-card
  [id]
  (let [alert @(rf/subscribe [:alert id])
        session    @(rf/subscribe [:session])]
    [:div
     (:device-id alert) "will alert if"
     (:metric-name alert) "is"
     (:condition alert)
     (:threshold alert)]))

(defn new-alert []
  (let [alert (atom {})
        ds @(rf/subscribe [:device-name-ids])]
    (fn []
      [:div.container
       [:div.row>div.col-sm-12
        [b/Form
         [b/FormGroup
          [b/Label {:for "Device"} "Device" ]
          (apply  vector
                  b/Input {:type "select"
                           :name "short-link"
                           :value (:device-id @alert)
                           :on-change #(swap! alert assoc-in [:device-id] (-> % .-target .-data))
                           :placeholder "adevice"}
                  (for [device ds]
                    [:option {:data (:id device)
                              :key (:id device)
                              } (:name device)]))]
         [b/FormGroup
          [b/Label {:for "metric-name"} "Metric Name"]
          [b/Input {:type "select"
                    :name "metric-name"
                    :value (:metric-name @alert)
                    :on-change #(swap! alert assoc-in [:metric-name] (-> % .-target .-value))
                    :placeholder "metric"}
           [:option "Temperature"]
           [:option "Humidity"]]
          [b/FormGroup
           [b/Label {:for "condition"} "condition"]
           [b/Input {:type "select"
                     :name "condition"
                     :value (:condition @alert)
                     :on-change #(swap! alert
                                        assoc-in
                                        [(keyword  (-> % .-target .-value))]
                                        true)
                     :placeholder "above"}
            [:option "above"]
            [:option "below"]]]
          [b/FormGroup
           [b/Label {:for "threshold"} "threshold"]
           [b/Input {:type "text"
                     :name "threshold"
                     :value (:threshold @alert)
                     :on-change #(swap! alert assoc-in [:threshold] (-> % .-target .-value))
                     :placeholder "50"}]]
          [b/FormGroup
           [b/Label {:for "phone"} "phone"]
           [b/Input {:type "text"
                     :name "phone"
                     :value (:phone @alert)
                     :on-change
                     #(swap! alert assoc-in
                             [:phone]
                             (-> % .-target .-value))
                     :placeholder "1-509-699-1184"}]]]]
        [b/Row
         [b/Col
          [b/Button
           {:on-click #(do
                         (swap! alert assoc-in [:created-on] (js/Date.))
                         (rf/dispatch [::post-alert @alert]))}
           "Add Alert"]]]]])))

(defn alerts-page []
  (let [alert-ids @(rf/subscribe [:alert-ids])
        session    @(rf/subscribe [:session])
        rows-of-three (partition-all 3 alert-ids)]
    (if (:email session)
      [b/Container
       (map-indexed
        (fn [index row]
          [b/Row {:key (str "alert-id-row-" index )}
           (for [alert-id row]
             [b/Col {:key (str "device-" alert-id) :style {"padding-top" "10px"} :sm "4"}
              [alert-card alert-id]])]) rows-of-three)
       [:div.container {:style {"border" "1px"}}
        [new-alert]]])))

