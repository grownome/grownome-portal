(ns grownome.owners
  (:require [baking-soda.core :as b]
            [kee-frame.core :as kf]
            [markdown.core :refer [md->html]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [grownome.devices :as devices]
            [grownome.ajax :as ajax]
            [grownome.routing :as routing]))


(kf/reg-controller
 ::owners-controller
 {:params (fn [{:keys [data path-params]}]
            (when (= :owners (:name data)) (constantly true)))
  :start  [::load-owners-page]})

(kf/reg-chain
 ::load-owners-page
 (fn [_ _]
   {:http {:method      :get
           :url         "/admin/owners"
           :error-event [:common/set-error]}})
 (fn [{:keys [db]} [_ owners]]
   {:db (assoc db :owners (into {} (map #(vector (:id %) %) owners)))})
 (fn [_ _]
   {:http {:method      :get
           :url         "/devices"
           :error-event [:common/set-error]}})
 (fn [{:keys [db]} [_ devices]]
   {:db (assoc db :devices (into {} (map #(vector (:id %) %) devices)) )})
 )

(kf/reg-chain
 ::post-owner
 (fn [_ [ owner]]
   {:http {:method      :post
           :url         "/admin/owners"
           :ajax-map {:params owner}
           :error-event [:common/set-error]}})
 (fn [{:keys [db]} [_ _ owner]]
   {:dispatch [::load-owners-page]}))


(rf/reg-sub
 :owner-ids
 (fn [db _]
   (keys (:owners db))))

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
 :owner
 (fn [db [_ id]]
   (get-in db [:owners id])))

(defn owner-card
  [id]
  (let [owner       (rf/subscribe [:owner id])
        session    @(rf/subscribe [:session])]
    (fn []
      (js/console.log @owner)
      [:div
       [:h4  (:device-id @owner) "is owned by" (:user-id @owner)]])))

(defn owner-table
  [id]
  (let [owner       (rf/subscribe [:owner id])
        device     @(rf/subscribe [:device id])
        session    @(rf/subscribe [:session])]
    (fn [id]
      (js/console.log @owner)
      [b/Table
       [:tbody
        [:tr
         [:td (:device-id @owner)]
         [:td (:resin-name device)]
         [:td " is owned by "]
         [:td (:user-id @owner)]
         ]
        ]]
      )
    ))


(defn new-owner []
  (let [ds (rf/subscribe [:device-name-ids])]
    (fn []
      (r/with-let [ds @ds
                   owner (r/atom
                          {})]
        [:div.container
         [:div.row>div.col-sm-12
          [b/Form
           [b/FormGroup
            [b/Label {:for "Device"} "Device" ]
            (apply  vector
                    b/Input {:type "select"
                             :name "device-id"
                             :value (:device-id @owner)
                             :on-change (fn [ctx ]
                                          (swap!
                                           owner
                                           assoc-in
                                           [:device-id]
                                           (-> ctx .-target .-value)))
                             :placeholder "adevice"}
                    (for [device ds]
                      [:option {:value (:id device)
                                :key (:id device)
                                } (:name device)]))]
            [b/FormGroup
             [b/Label {:for "email"} "email"]
             [b/Input {:type "text"
                       :name "threshold"
                       :value (:email @owner)
                       :on-change #(swap!
                                    owner
                                    assoc-in
                                    [:email]
                                    (-> % .-target .-value))
                       :placeholder "owner@owner.com"}]]]
          [b/Row
           [b/Col
            [b/Button
             {:on-click #(do
                           (swap! owner assoc-in [:created-on] (js/Date.))
                           (rf/dispatch [::post-owner @owner]))}
             "Add New Owner"]]]]]))))

(defn owners-page []
  (let [owner-ids @(rf/subscribe [:owner-ids])
        session    @(rf/subscribe [:session])
        rows-of-three (partition-all 3 owner-ids)]
    (js/console.log owner-ids)
    (if (:email session)
      [b/Container
       [b/Table
       [:thead
        [:tr
         [:th "Device ID"]
         [:th "Resin Name"]
         [:th " "]
         [:th "User"]]]]
       (map-indexed
        (fn [index owner-id]
          [b/Row {:key (str "owner-id-row-" index )}
           [owner-table owner-id]]) owner-ids)
       [:div.container {:style {"border" "1px"}}
        [:br]
        [new-owner]]])))


