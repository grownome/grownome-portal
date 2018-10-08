(ns grownome.device
  (:require [baking-soda.core :as b]
            [kee-frame.core :as kf]
            [markdown.core :refer [md->html]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [grownome.ajax :as ajax]
            [grownome.routing :as routing]))

(kf/reg-controller
  ::device-controller
  {:params (constantly true)
   :start  [::load-device-page]})


(kf/reg-chain
 ::load-device-page
 (fn [_ _]
   {:http {:method      :get
           :url         "/device"
           :error-event [:common/set-error]}})
 (fn [{:keys [db]} [_ devices]]
   {:db (assoc db :devices devices)}))
