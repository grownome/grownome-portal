(ns grownome.core
  (:require [baking-soda.core :as b]
            [kee-frame.core :as kf]
            [markdown.core :refer [md->html]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [grownome.analytics.mixpanel :as mix]
            [grownome.profile :as profile]
            [grownome.devices :as devices]
            [grownome.alerts :as alerts]
            [grownome.owners :as owners]
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
                   :class-name "navbar-dark bg-dark"
                   :expand "md"}
         [b/NavbarBrand {:href "/"} [:img {:src "/img/Logo_small_green.png"}]]
         [b/NavbarToggler {:on-click #(swap! expanded? not)}]
         [b/Collapse {:is-open @expanded? :navbar true}
          (if (nil? (:email @session))
            [b/Nav {:class-name "mr-auto" :navbar true}
             [nav-link "Home" :home]
             [nav-link "About" :about]
             [b/NavLink  {:href "/auth/init" :class-name "right"} "Sign-in"]]
            [b/Nav {:class-name "mr-auto" :navbar true}
             [nav-link "Home"    :home]
             [nav-link "Devices" :devices]
             (when (:admin @session ) [nav-link "Owners"  :owners])
             (when (:admin @session ) [nav-link "Alerts"  :alerts])
             [nav-link "About"   :about]
             [b/NavLink  {:href "/auth/out" :class-name "right"} "Sign-out"]])]]))))


(rf/reg-sub
 :loading
 (fn [db _]
   (:loading db)))

(rf/reg-sub
 :errors
 (fn [db _]
   (:errors db)))

(rf/reg-event-db
 :common/set-error
 (fn [db [_ e]]
   (update db :errors #(conj % e))))

(rf/reg-event-db
 :unset-error
 (fn [db [_ e]]
   (update db :errors filter #(not (= e %)))))

(rf/reg-event-db
 :set-loading
 (fn [db [_ token]]
   (update db :loading  #(conj (or % #{}) token))))

(rf/reg-event-db
 :unset-loading
 (fn [db [_ e]]
   (update db :loading #(disj % e))))


(defn alertbar []
  (let [loading (rf/subscribe [:loading])
        session @(rf/subscribe [:session])
        errors  (rf/subscribe [:errors])]
    (fn []
      [:div.alert-bar
       (conj
        [:div.col-md-12]
        (for [e @errors]
          [b/Alert {:color "error"
                    :key (rand-int 10000)}
           (get e :status-text)]))
       (conj
        [:div.loading]
        (for [l @loading]
          [b/Alert
           {:color "dark"}
           (str "loading " )l]))])))

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
    [:h4 "Thank you for using and testing Grownome. Please send bug
reports, feature requests and any other information to "
     [b/Badge {:href "mailto:support@grownome.com" :color "info" :class "h2"}
      (str "support@grownome.com")]
     " or through the chat box at the bottom right."]]
   [:br]
   [b/CardDeck
   [b/Card  {:style {"overflow"  "hidden"
                     "max-width" "300px"}}
    [b/CardBody {:style {"paddingTop" "20px"
                         "overflow" "hidden"}}
     [b/CardTitle "Newest Update"]
     [:h2 "Left and right arrows allow for precise photo selection"]]]
   [b/Card  {:style {"overflow"  "hidden"
                     }}
    [b/CardBody {:style {"paddingTop" "20px"
                         "overflow" "hidden"}}
     [b/CardTitle "Alpha Feature"]
     [:h2 "'Dryness Predictor' analyzes your images to predict how dry your plants are. Try it out and let us know what you think of it. This is a beta feature that we are constantly improving."]]]]
   [:br]
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
   [alertbar]
   [kf/switch-route
    (fn [route]
      (mix/track-pageview (get-in route [:path]))
      (mix/track (str (get-in route [:data :name])))
      (get-in route [:data :name]))
    :home [home-page]
    :about [about-page]
    :devices [devices/devices-page]
    :alerts   [alerts/alerts-page]
    :owners   [owners/owners-page]
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
