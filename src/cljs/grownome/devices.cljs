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
           :url         "/device"
           :error-event [:common/set-error]}})
 (fn [{:keys [db]} [_ devices]]
   {:db (assoc db :devices devices)}))

(kf/reg-chain
 ::post-device
 (fn [_ [ device]]
   {:http {:method      :post
           :url         "/device"
           :ajax-map {:body  device}
           :error-event [:common/set-error]}})
 (fn [{:keys [db]} [_ devices]]
   {:db (assoc db :devices devices)}))


(defn device-card
  [id]
    [b/Card
     [b/CardImg {:top true
                 :width "100%"
                 :src "https://placeholdit.imgix.net/~text?txtsize=33&txt=318%C3%97180&w=318&h=180"

                 }]
     [b/CardBody
      [:div
       [b/CardTitle "Device1"]
       [:div "time slider"]
       [b/Input {:type "range" :min 1 :max 100}]
       [b/Button "metrics"]
       [b/Button "Images"]]
      ]
     ]
  )

(defn new-device []
  (let [device (atom {})

        ]
    [:div.container
     [:div.row>div.col-sm-12
      [b/Form 
       [b/FormGroup
        [b/Label {:for "iot_id"} "IOT Numeric ID"]
        [b/Input {:type "text"
                  :name "id"
                  :value (:id @device)
                  :on-change #(swap! device assoc-in [:id] (-> % .-target .-value))
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
                  :value (:resin-name @device)
                  :on-change #(swap! device assoc-in [:resin-name] (-> % .-target .-value))
                  :placeholder "broken-sunrise"}]]
       [b/FormGroup
        [b/Label {:for "short_link"} "Shortlink"]
        [b/Input {:type "text"
                  :name "short_link"
                  :value (:short-link @device)
                  :on-change #(swap! device assoc-in [:short-link] (-> % .-target .-value))
                  :placeholder "nome.run/adevice"}]]]]
     [b/Row
      [b/Col
       [b/Button
        { :on-click #(rf/dispatch [::post-device @device])}
        "Add Device"
        ]]]])
  )
(defn devices-page []
  [b/Container
   [b/Row
    [b/Col
     [device-card 1]]
    [b/Col
     [device-card 1]]
    [b/Col
     [device-card 1]]]

   [:div.container
    [new-device]
    ]
   
     ])

