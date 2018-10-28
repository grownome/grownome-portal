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
            [grownome.analytics.mixpanel :as mix]
            [grownome.routing :as routing]))

(kf/reg-controller
 ::profile-controller
 {:params (constantly true)
  :start  [::load-profile-page]})

(defn start-intercom
  [profile]
  (js/Intercom "boot"
               #js
               {"app_id" "l9gd6itn",
                "email" (:email profile)
                "user_id" (:id profile)
                "admin" (or (:admin profile) false)}))

(kf/reg-chain
 ::load-profile-page
 (fn [_ _]
   {:http {:method      :get
           :url         "/profile"
           :error-event [:common/set-error]}})
 (fn [{:keys [db]} [_ profile]]
   (when (not (=  (:profile db) profile))
     (js/console.log "time to update")
     (js/console.log profile)
     (start-intercom profile)
     (mix/identify (:id profile))
     (mix/name-tag (:email profile))
     (mix/set-people-props
      {"$name"  (:email profile)
       "$email" (:email profile)
       "admin"  (or (:admin profile) false)}
      ))
   {})
 (fn [{:keys [db]} [_ profile _ ]]
   (js/console.log "debuging profile")
   (js/console.log profile)
   {:db (assoc db :profile profile)})


 )

(defn profile-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     [:img {:src "/img/warning_clojure.png"}]]]])
