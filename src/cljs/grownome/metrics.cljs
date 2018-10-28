(ns grownome.metrics
  (:require [baking-soda.core :as b]
            [kee-frame.core :as kf]
            [markdown.core :refer [md->html]]
            [reagent.core :as r]
            [goog.string]
            [cognitect.transit :as t]
            [re-frame.core :as rf]
            [grownome.datetime :as datetime]
            [goog.date.UtcDateTime :as dates]
            [grownome.analytics.mixpanel :as mix]
            [grownome.chart :as chart]
            [grownome.ajax :as ajax]
            [ajax.util :as util]
            [goog.Uri.QueryData]
            [grownome.routing :as routing]))

(kf/reg-controller
 ::device-metrics-controller
 {:params (fn [{:keys [data path-params query-string]}]
            (let [parsed-query (goog.Uri.QueryData. query-string)
                  query-map (zipmap (.getKeys parsed-query)
                                    (.getValues parsed-query))]
              (js/console.log (goog.Uri.QueryData. query-string))
              (when  (= :metrics (:name data))
                [(:id path-params) query-map])))
  :start (fn [ctx [id query-map]]
           [::load-device-metrics-page
            id
            (get query-map "after")
            (get query-map "interval")
            ])})

(defn default-after
  []
  (- (datetime/unix-timestamp)  (* 7 datetime/day)))

(def default-interval
  (* 10 datetime/minute))

(kf/reg-chain
 ::load-device-metrics-page
 (fn [{:keys [db]} [device-id after interval]]
   {:dispatch [:set-loading :metrics]
    :http {:method      :get
           :url         (str "/device/" device-id "/metrics")
           :ajax-map    {:params {:after (or after
                                             (default-after))
                                  :interval
                                  (or interval
                                      default-interval)}}
           :error-event [:common/set-error]}})
 (fn [{:keys [db]} [device-id after interval metrics]]
   {:db
    (-> db
        (assoc-in [::after] (or (js/parseInt after) (default-after)))
        (assoc-in [::interval] (or (js/parseInt interval) default-interval))
        (assoc-in [:focused-device] (js/parseInt device-id))
        (assoc-in [:devices (:id metrics) :metrics]
                  (:metrics metrics)))})
 (fn [_ _]
   {:dispatch [:unset-loading :metrics]}))

(rf/reg-sub
 :focused-device
 (fn [db _]
   (get-in db [:focused-device ])))

(rf/reg-sub
 :loading-metrics
 (fn [db _]
   (get-in db [:loading :metrics] false)))


(rf/reg-event-fx
 ::update-interval
 (fn [_ [_ device-id interval after]]
   (js/console.log  device-id)
   {:navigate-to [  :metrics  {:id device-id
                               :query-string (str "after="
                                                  (or after (default-after))
                                                  "&interval="
                                                  (or interval default-interval))
                               }]}))

(rf/reg-sub
 ::interval
 (fn [db _]
   [(get-in db [::interval]) (get-in db [::after])]))

(defn get-metric-from-device
  [device metric-name ]
   (into [] (map (keyword metric-name) (get-in device [:metrics metric-name])))
   )

(defn get-label-from-device
  [device metric-name]
  (let [v
        (into [] (map (fn [metric]
                        (when (:timestamp metric)
                          (datetime/short-datetime
                            (js/goog.date.UtcDateTime.
                             (clj->js  (:timestamp metric))))))
                      (get-in device [:metrics metric-name])))]

    (js/console.log v)
    v))

(defn on-updater
  [meta-data target k]
  (swap! target assoc k meta-data))

(def time-divisions
  [{:interval (* 10 datetime/minute) :string "10 minutes"}
   {:interval (* 30 datetime/minute) :string "30 minutes"}
   {:interval (* 1  datetime/hour)   :string "1 hour"}
   {:interval (* 2  datetime/hour)   :string "2 hours"}
   {:interval (* 4  datetime/hour)   :string "4 hours"}
   {:interval (* 12 datetime/hour)   :string "12 hours"}
   {:interval (* 1 datetime/day)     :string "1 day"}])

(def afters
  [{:interval (* 10 datetime/hour) :string "6 hours ago"}
   {:interval (* 30 datetime/hour) :string "12 hours ago"}
   {:interval (* 1  datetime/day)   :string "yesterday"}
   {:interval (* 2  datetime/day)   :string "2 days ago"}
   {:interval (* 4  datetime/day)   :string "4 days ago"}
   {:interval (* 7 datetime/day)   :string "1 week ago"}
   {:interval (* 14 datetime/day)      :string "2 weeks ago"}
   {:interval (* 1 datetime/month)     :string "1 day ago"}
   {:interval (datetime/unix-timestamp)     :string "forever"}])


(defn dropdown-interval-item
  [device-id state-atom meta-data]
  [b/DropdownItem {:on-click
                   (fn [e]
                     (mix/track (str "pick_metric_interval_" (:interval meta-data)))
                     (on-updater meta-data state-atom :interval)
                     (rf/dispatch [::update-interval
                                   device-id
                                   (:interval (:interval @state-atom))
                                   (:after (:after @state-atom))
                                   ])
                     )} (:string meta-data)])


(defn dropdown-after-item
  [device-id state-atom meta-data]
  [b/DropdownItem {:on-click
                   (fn [e]
                     (let [meta-data (assoc meta-data
                                            :after
                                            (- (datetime/unix-timestamp)
                                               (:interval meta-data)))]
                       (js/console.log meta-data)
                       (mix/track (str "pick_metric_after_" (:interval meta-data)))
                       (on-updater meta-data state-atom :after)
                       (rf/dispatch [::update-interval
                                     device-id
                                     (:interval (:interval @state-atom))
                                     (:after (:after @state-atom))])))}
   (:string meta-data)])
(defn time-picker
  [device-id]
  (let [[interval after] @(rf/subscribe [::interval])]
    (fn [device-id]
      (r/with-let [drop-down-toggle (r/atom false)
                   drop-down-after-toggle (r/atom false)
                   drop-down-state (r/atom {:interval
                                            {:interval interval
                                             :string (datetime/as-duration
                                                      (* 1000 interval))
                                             }
                                            :after
                                            {:string (datetime/short-datetime
                                                      (* 1000 (clj->js after)))
                                             :after
                                             after} })]
        [b/Container
         [b/Row
          "Interval: "
          [b/Dropdown {:is-open @drop-down-toggle
                       :toggle #(swap! drop-down-toggle not)}
           [b/DropdownToggle {:class ["caret"]}
            (or  (:string (:interval @drop-down-state)) "Pick")]
           (apply vector b/DropdownMenu
                  (map (partial dropdown-interval-item device-id drop-down-state )
                       time-divisions))]

          "After: "

          [b/Dropdown {:is-open @drop-down-after-toggle
                       :toggle #(swap! drop-down-after-toggle not)}
           [b/DropdownToggle {:class ["caret"]}
            (or  (:string (:after @drop-down-state)) "Pick")]
           (apply vector b/DropdownMenu
                  (map (partial dropdown-after-item device-id drop-down-state)
                       afters))]
          ]]))))

(defn device-metrics-card
  [id metric-name]
  (let [device     (rf/subscribe [:device id])
        session    @(rf/subscribe [:session])
        ]
    (fn [id]
      (js/console.log @device)
      (r/with-let [metrics    (reverse (get-metric-from-device @device metric-name))
                   labels     (reverse (get-label-from-device @device metric-name))]
        [b/Card
         [b/CardTitle
          [chart/chart labels metric-name metrics]]
         [b/CardBody
          [:div
           [b/CardTitle  (:name device) ]
           (when (:admin session)
             [b/CardTitle (:resin_name device)])
           (when (:admin session)
             [b/CardTitle (:id device)])]]]))))

(defn metrics-page []
  (let [device-id (rf/subscribe [:focused-device])
        loading   (rf/subscribe [:loading-metrics])
        session   (rf/subscribe [:session])]
    (fn []
      (when (and (not @loading) (:email @session))
        [b/Container
         [time-picker @device-id]
         [device-metrics-card @device-id "temperature"]
         [:div {:style {"paddingTop" "10px"}}]
         [device-metrics-card @device-id "humidity"]]))))

