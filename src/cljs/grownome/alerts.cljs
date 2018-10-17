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
            (when (= :alerts (:name data)) (constantly true)))
  :start  [::load-alerts-page]})

(kf/reg-chain
 ::load-alerts-page
 (fn [_ _]
   {:http {:method      :get
           :url         "/alerts"
           :error-event [:common/set-error]}})
 (fn [{:keys [db]} [_ alerts]]
   {:db (assoc db :alerts (into {} (map #(vector (:id %) %) alerts)))})
 (fn [_ _]
   {:http {:method      :get
           :url         "/devices"
           :error-event [:common/set-error]}})
 (fn [{:keys [db]} [_ _ devices]]
   {:db (assoc db :devices (into {} (map #(vector (:id %) %) devices)) )}))

(kf/reg-chain
 ::post-alert
 (fn [_ [ alert]]
   {:http {:method      :post
           :url         "/alerts"
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
  (let [alert       (rf/subscribe [:alert id])
        session    @(rf/subscribe [:session])]
    (fn []
      (js/console.log @alert)
      [:div
       [:h4  (:device-id @alert) " will alert if "
        (:metric-name @alert) " is "
        (:condition @alert) " "
        (:threshold @alert)]])))

(defn new-alert []
  (let [
        ds (rf/subscribe [:device-name-ids])
        
        ]

    (fn []
      (r/with-let [ds @ds
                   alert (r/atom {:device-id (:id (first ds))
                                 :metric-name "temperature"
                                 :condition "above"
                                 })] 
        (js/console.log (first ds))
        [:div.container
         [:div.row>div.col-sm-12
          [b/Form
           [b/FormGroup
            [b/Label {:for "Device"} "Device" ]
            (apply  vector
                    b/Input {:type "select"
                             :name "device-id"
                             :value (:device-id @alert)
                             :on-change (fn [ctx ]
                                          (swap!
                                           alert
                                           assoc-in
                                           [:device-id]
                                           (-> ctx .-target .-value)))
                             :placeholder "adevice"}
                    (for [device ds]
                      [:option {:value (:id device)
                                :key (:id device)
                                } (:name device)]))]
           [b/FormGroup
            [b/Label {:for "metric-name"} "Metric Name"]
            [b/Input {:type "select"
                      :name "metric-name"
                      :value (:metric-name @alert)
                      :on-change #(swap! alert assoc-in
                                         [:metric-name]
                                         (-> % .-target .-value))
                      :placeholder "metric"}
             [:option {:value "temperature"} "Temperature"]
             [:option {:value "humidity"} "Humidity"]]
            [b/FormGroup
             [b/Label {:for "condition"} "condition"]
             [b/Input {:type "select"
                       :name "condition"
                       :value (:condition @alert)
                       :on-change #(swap! alert
                                          assoc-in
                                          [:condition]
                                          (-> % .-target .-value))
                       :placeholder "above"}
              [:option {:value "above"} "above"]
              [:option {:value "below"} "below"]]]
            [b/FormGroup
             [b/Label {:for "threshold"} "threshold"]
             [b/Input {:type "text"
                       :name "threshold"
                       :value (:threshold @alert)
                       :on-change #(swap!
                                    alert
                                    assoc-in
                                    [:threshold]
                                    (-> % .-target .-value))
                       :placeholder "Must be whole positive number 50"}]]
            [b/FormGroup
             [b/Label {:for "description"} "description"]
             [b/Input {:type "text"
                       :name "description"
                       :value (:description @alert)
                       :on-change #(swap!
                                    alert
                                    assoc-in
                                    [:description]
                                    (-> % .-target .-value))
                       :placeholder "What is this alert for?"}]]
            [b/FormGroup
             [b/Label {:for "phone"} "phone"]
             [b/Input {:type "text"
                       :name "phone"
                       :value (:phone-number @alert)
                       :on-change
                       #(swap! alert assoc-in
                               [:phone-number]
                               (-> % .-target .-value))
                       :placeholder "1-501-555-1111"}]]]]
          [b/Row
           [b/Col
            [b/Button
             {:on-click #(do
                           (swap! alert assoc-in [:created-on] (js/Date.))
                           (rf/dispatch [::post-alert @alert]))}
             "Add Alert"]]]]]))))

(defn alerts-page []
  (let [alert-ids @(rf/subscribe [:alert-ids])
        session    @(rf/subscribe [:session])
        rows-of-three (partition-all 3 alert-ids)]
    (js/console.log alert-ids)
    (if (:email session)
      [b/Container
       (map-indexed
        (fn [index row]
          [b/Row {:key (str "alert-id-row-" index )}
           (for [alert-id row]
             [b/Col {:key (str "device-" alert-id) :style {"paddingTop" "10px"} :sm "4"}
              [alert-card alert-id]])]) rows-of-three)
       [:div.container {:style {"border" "1px"}}
        [new-alert]]])))

