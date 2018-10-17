(ns grownome.core
  (:require [baking-soda.core :as b]
            [kee-frame.core :as kf]
            [markdown.core :refer [md->html]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [grownome.profile :as profile]
            [grownome.devices :as devices]
            [grownome.alerts :as alerts]
            [grownome.metrics :as metrics]
            [grownome.ajax :as ajax]
            [grownome.routing :as routing])
  (:import goog.History))

; the navbar components are implemented via baking-soda [1]
; library that provides a ClojureScript interface for Reactstrap [2]
; Bootstrap 4 components.
; [1] https://github.com/gadfly361/baking-soda
; [2] http://reactstrap.github.io/

(defn nav-link [title page]
  [b/NavItem
   [b/NavLink
    {:href   (kf/path-for [page])
     :active (= page @(rf/subscribe [:nav/page]))}
    title]])

(defn navbar []
  (let [session (rf/subscribe [:session])]
    (fn []
      (r/with-let [expanded? (r/atom true)
                   ]
        [b/Navbar {:light true
                   :class-name "navbar-dark bg-primary"
                   :expand "md"}
         [b/NavbarBrand {:href "/"} "grownome"]
         [b/NavbarToggler {:on-click #(swap! expanded? not)}]
         [b/Collapse {:is-open @expanded? :navbar true}
          (if (nil? (:email @session))
            [b/Nav {:class-name "mr-auto" :navbar true}
             [nav-link "Home" :home]
             [nav-link "About" :about]
             [b/NavLink  {:href "/auth/init"} "Sign-in"]]
            [b/Nav {:class-name "mr-auto" :navbar true}
             [nav-link "Home"    :home]
             [nav-link "Devices" :devices]
             (when (:admin @session ) [nav-link "Alerts"  :alerts])
             [nav-link "About"   :about]
             [b/NavLink  {:href "/auth/out"} "Sign-out"]
             ])]]))))

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     [:img {:src "/img/warning_clojure.png"}]]]])

(rf/reg-event-fx
  ::load-about-page
  (constantly nil))

(rf/reg-sub
  :docs
  (fn [db _]
    (:docs db)))

(defn home-page []
  [:div.container
   [:div.row>div.col-sm-12
    [:h2.alert.alert-info "Tip: try pressing CTRL+H to open re-frame tracing menu"]]
   (when-let [docs @(rf/subscribe [:docs])]
     [:div.row>div.col-sm-12
      [:div ]])])

(kf/reg-chain
  ::load-session
  (fn [_ _]
    {:http {:method      :get
            :url         "/profile"
            :error-event [:common/set-error]}})
  (fn [{:keys [db]} [_ session]]
    {:db (assoc db :session session)}))


(kf/reg-controller
 ::session-controller
 {:params (constantly true)
  :start  [::load-session]})


(rf/reg-sub
 :session
 (fn [db _]
   (:session db)))

(defn root-component []
  [:div
   [navbar]
   [kf/switch-route (fn [route] (get-in route [:data :name]))
    :home [home-page]
    :about [about-page]
    :devices [devices/devices-page]
    :alerts   [alerts/alerts-page]
    :profile  [profile/profile-page]
    :metrics  [metrics/metrics-page]
    nil [:div ""]]])

;; -------------------------
;; Initialize app
(defn mount-components []
  (rf/clear-subscription-cache!)
  (r/render [#'root-component] (.getElementById js/document "app")))

(defn init! []
  (ajax/load-interceptors!)
  (kf/start! {:debug?         true
              :router         (routing/->ReititRouter routing/router)
              :chain-links    [ajax/ajax-chain]
              :initial-db     {}
              :root-component [root-component]})
  (routing/hook-browser-navigation!))
