(ns grownome.device
  (:require [baking-soda.core :as b]
            [kee-frame.core :as kf]
            [markdown.core :refer [md->html]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [grownome.ajax :as ajax]
            [grownome.routing :as routing]))

(kf/reg-chain
 ::load-device-page
 (fn [_ [device-id]]
   {:http {:method      :get
           :url         (str "/device/"  device-id)
           :error-event [:common/set-error]}})
 (fn [{:keys [db]} [_ devices]]
   {:db (assoc db :devices devices)}))
