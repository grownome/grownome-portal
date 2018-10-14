(ns grownome.devices
  (:require [baking-soda.core :as b]
            [kee-frame.core :as kf]
            [markdown.core :refer [md->html]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [grownome.ajax :as ajax]
            [grownome.routing :as routing]))

(kf/reg-controller
 ::devices-controller
 {:params (constantly true)
  :start  [::load-devices-page]})

(kf/reg-chain
 ::load-devices-page
 (fn [_ _]
   {:http {:method      :get
           :url         "/devices"
           :error-event [:common/set-error]}})
 (fn [{:keys [db]} [_ devices]]
   {:db (assoc db :devices (into {} (map #(vector (:id %) %) devices)) )}))

(kf/reg-chain
 ::post-device
 (fn [_ [ device]]
   {:http {:method      :post
           :url         "/device"
           :ajax-map {:params device}
           :error-event [:common/set-error]}})
 (fn [{:keys [db]} [_ device]]
   {:dispatch [::load-devices-page]}))


(rf/reg-sub
 :device-ids
 (fn [db _]
   (keys (:devices db))))

(rf/reg-sub
 :device
 (fn [db [_ id]]
   (get-in db [:devices id])))

(defn device-card
  [id]
  (let [device     @(rf/subscribe [:device id])
        session    @(rf/subscribe [:session])
        image-slider-value (r/atom 0)]
    (fn [id]
      [b/Card
       (when (not-empty  (:images device) )
         [b/CardImg {:top true
                     :width "100%"
                     :src (get  (nth (:images device) @image-slider-value) :path "test")

                     }])
       [b/CardBody
        [:div
         [b/CardTitle (:name device)]
         (when (:admin session)
           [b/CardTitle (:resin_name device)])
         (when (:admin session)
           [b/CardTitle (:id device)])
         [:div "time slider"]

         [b/Input {:type "range"
                   :value @image-slider-value
                   :on-change #(reset! image-slider-value (js/parseInt (-> % .-target .-value)))
                   :min 0
                   :max (dec (count (:images device)))}]
         [b/Button "metrics"]
         [:div (str "total image count: " (count (:images device)))]
         " "
         [b/Button "Images"]
         " "
         [b/Button {:color "danger"} "Delete"]
         ]
        ]
       ])))

(defn new-device []
  (let [device (atom {})]
    [:div.container
     [:div.row>div.col-sm-12
      [b/Form
       [b/FormGroup
        [b/Label {:for "iot_id"} "IOT Numeric ID"]
        [b/Input {:type "text"
                  :name "id"
                  :value (:id @device)
                  :on-change #(swap! device assoc-in [:id] (js/parseInt (-> % .-target .-value)))
                  :placeholder "12392811232"}]]
       [b/FormGroup
        [b/Label {:for "name"} "Device Name"]
        [b/Input {:type "text"
                  :name "name"
                  :value (:name @device)
                  :on-change #(swap! device assoc-in [:name] (-> % .-target .-value))
                  :placeholder "Grow Box (not unique)"}]]
       [b/FormGroup
        [b/Label {:for "resin_name"} "Resin Name"]
        [b/Input {:type "text"
                  :name "resin_name"
                                        ;These are underscores to play nice with the db.
                  ;There are some docs on how to make them change automatically
                  :value (:resin_name @device)
                  :on-change #(swap! device assoc-in [:resin_name] (-> % .-target .-value))
                  :placeholder "broken-sunrise"}]]
       [b/FormGroup
        [b/Label {:for "short_link"} "Shortlink"]
        [b/Input {:type "text"
                  :name "short_link"
                  :value (:short_link @device)
                  :on-change #(swap! device assoc-in [:short_link] (-> % .-target .-value))
                  :placeholder "nome.run/adevice"}]]]]
     [b/Row
      [b/Col
       [b/Button
        { :on-click #(do
                       (swap! device assoc-in [:created_on] (js/Date.))

                       (rf/dispatch [::post-device @device]))}
        "Add Device"
        ]]]]))

(defn devices-page []
  (let [device-ids @(rf/subscribe [:device-ids])
        session    @(rf/subscribe [:session])
        rows-of-three (partition-all 3 device-ids)
        ]
    [b/Container
     (map-indexed
      (fn [index row]
        [b/Row {:key (str "device-id-row-" index )}
         (for [device-id row]
           [b/Col {:key (str "device-" device-id) :sm "4"}
            [device-card device-id]])]) rows-of-three)
     (if (:admin session)
       [:div.container {:style {"border" "1px"}}
        [new-device]])]))

