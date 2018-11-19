(ns grownome.devices
  (:require [baking-soda.core :as b]
            [kee-frame.core :as kf]
            [markdown.core :refer [md->html]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [grownome.ajax :as ajax]
            [grownome.analytics.mixpanel :as mix]
            [grownome.routing :as routing]))

(kf/reg-controller
 ::devices-controller
 {:params (fn [{:keys [data path-params]}]
            (when (= :devices (:name data)) true))
  :start  [::load-devices-page]})

(kf/reg-chain
 ::load-devices-page
 (fn [_ _]
   {:dispatch [:set-loading :devices]
    :http {:method      :get
           :url         "/devices"
           :error-event [:common/set-error]}})
 (fn [{:keys [db]} [_ devices]]
   {:dispatch [:unset-loading :devices]
    :db (assoc db :devices (into {} (map #(vector (:id %) %) devices)) )}))


(kf/reg-chain
 ::post-device
 (fn [_ [device]]
   {:http {:method      :post
           :url         "/admin/device"
           :ajax-map {:params device}
           :error-event [:common/set-error]}})
 (fn [{:keys [db]} [_ device]]
   {:dispatch [::load-devices-page]}))

(kf/reg-chain
 ::get-prediction
 (fn [_ [image-url]]
   (mix/track "predicted")
   {:http {:method      :get
           :url         "/predict"
           :ajax-map {:params  {:url image-url}}
           :error-event [:common/set-error]}})
 (fn [{:keys [db]} [_ prediction]]
   {:db (assoc-in db [:predictions (:url prediction)] prediction)}))



(rf/reg-sub
 :device-ids
 (fn [db _]
   (keys (:devices db))))

(rf/reg-sub
 :device-name-ids
 (fn [db _]
   (into []
         (map (fn [[i device]]
                {:id i
                 :name  (:name device)})
              (:devices db)))))



(rf/reg-sub
 :device
 (fn [db [_ id]]
   (get-in db [:devices id])))

(rf/reg-sub
 :predictions
 (fn [db [_]]
   (get-in db [:predictions])))

(defn dec-min
  [v min]
  (if (= v min )
    v
    (dec v)))

(defn inc-max
  [v max]
  (if (= v max)
    v
    (inc v)))



(defn device-card
  [id]
  (let [device     @(rf/subscribe [:device id])
        session    @(rf/subscribe [:session])
        predictions (rf/subscribe [:predictions])
        image-slider-value (r/atom 0)]
    (fn [id]
      [b/Card  {:style {"overflow"  "hidden"}}

       (when (not-empty  (:images device) )
         [b/CardImg {:top true
                     :width "100%"
                     :height "100%"
                     :href (get  (nth (:images device) @image-slider-value) :path "test")
                     :src (get  (nth (:images device) @image-slider-value) :path "test")}])
       [b/CardBody {:style {"paddingTop" "20px"
                            "overflow" "hidden"}}
        [:div
         [b/CardTitle  (:name device) ]
         (when (:admin session)
           [b/CardTitle (:resin-name device)])
         (when (:admin session)
           [b/CardTitle (:id device)])
         (when (not-empty (:images device))
           [:div (str "created on: "
                      (get  (nth (:images device) @image-slider-value) :created-on))])
         [:br]
         [:div "Image Slider"]
         [b/Input {:type "range"
                   :value @image-slider-value
                   :on-change #(reset! image-slider-value (js/parseInt (-> % .-target .-value)))
                   :min 0
                   :max (dec (count (:images device)))}]
         [:div (str "total image count: " (count (:images device)))]
         [:br]
         [:div
          [b/Button {:on-click #(swap! image-slider-value dec-min 0)} "<"]
          " " " "
          [b/Button {:href (kf/path-for
                            [:metrics {:id (:id device)}])} "metrics" ]
          " "
          [b/Button {:on-click #(swap! image-slider-value inc-max (dec (count  (:images device))))} ">"]]
         " "
         [:br]
         [b/Button {:on-click
                    (fn []
                      (let [slider
                            (get (nth
                                  (:images device)
                                  @image-slider-value) :path)]
                        (rf/dispatch [::get-prediction
                                      slider
                                      ])))}
          "Predict Dryness"]
         " "
         (when (:admin session)
           (when-let  [prediction (if (empty? (:images device))
                                    (== nth nil)
                                    (get @predictions
                                       (get (nth
                                             (:images device)
                                             @image-slider-value) :path)))]
             [:div "something should be here"]
             [b/CardBody 
              [:div { }
               (map (fn [p]
                      [:div {:key
                             (str (:id device) "-" (:label p) "pred")}
                       [b/CardBody (:label p)]
                       [b/Progress {:value (* 100 (:score p))
                                    :key (str (:id device) "-" (:label p) "prod")}
                        (:score p)
                        " "
                        ]]

                      )
                    (:labels prediction))]]))
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
        [b/Label {:for "iot-id"} "IOT Numeric ID"]
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
        [b/Label {:for "resin-name"} "Resin Name"]
        [b/Input {:type "text"
                  :name "resin-name"
                                        ;These are underscores to play nice with the db.
                  ;There are some docs on how to make them change automatically
                  :value (:resin-name @device)
                  :on-change #(swap! device assoc-in [:resin-name] (-> % .-target .-value))
                  :placeholder "broken-sunrise"}]]
       [b/FormGroup
        [b/Label {:for "short-link"} "Shortlink"]
        [b/Input {:type "text"
                  :name "short-link"
                  :value (:short-link @device)
                  :on-change #(swap! device assoc-in [:short-link] (-> % .-target .-value))
                  :placeholder "nome.run/adevice"}]]]]
     [b/Row
      [b/Col
       [b/Button
        { :on-click #(do
                       (swap! device assoc-in [:created-on] (js/Date.))

                       (rf/dispatch [::post-device @device]))}
        "Add Device"
        ]]]]))

(defn devices-page []
  (let [device-ids @(rf/subscribe [:device-ids])
        session    @(rf/subscribe [:session])
        rows-of-three (partition-all 3 device-ids)
        ]
    (if (:email session)
      [b/Container
       (map-indexed
        (fn [index row]
          [b/Row {:key (str "device-id-row-" index )}
           (for [device-id row]
             [b/Col {:key (str "device-" device-id) :style {"paddingTop" "10px"} :sm "4"}
              [device-card device-id]])]) rows-of-three)
       (if (:admin session)
         [:div.container {:style {"border" "1px"}}
          [new-device]])])))

