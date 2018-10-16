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
 ::profile-controller
 {:params (constantly true)
  :start  [::load-profile-page]})

(kf/reg-chain
 ::load-profile-page
 (fn [_ _]
   {:http {:method      :get
           :url         "/profile"
           :error-event [:common/set-error]}})
 (fn [{:keys [db]} [_ profile]]
   {:db (assoc db :profile profile)}))

(defn profile-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     [:img {:src "/img/warning_clojure.png"}]]]])
