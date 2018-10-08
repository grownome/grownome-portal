(ns grownome.profile
  (:require [baking-soda.core :as b]
            [kee-frame.core :as kf]
            [markdown.core :refer [md->html]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [grownome.ajax :as ajax]
            [grownome.routing :as routing]
            [baking-soda.core :as b]
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
   {:db (assoc db :devices devices)}))

(defn profile-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     [:img {:src "/img/warning_clojure.png"}]]]])
