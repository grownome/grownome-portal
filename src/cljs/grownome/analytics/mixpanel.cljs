; found here https://github.com/PrecursorApp/precursor/blob/master/src-cljs/frontend/analytics/mixpanel.cljs
(ns grownome.analytics.mixpanel
  (:require [cljs.core.async :as async :refer [>! <! put! alts! chan sliding-buffer close!]]
            [grownome.datetime :refer [unix-timestamp]]
            ))

(defn track [event & [props]]
  (js/mixpanel.track event (clj->js (merge {:event_time (unix-timestamp)} props))))

(defn track-pageview [path]
  (js/mixpanel.track_pageview (clj->js path)))

(defn register-once [props]
  (js/mixpanel.register_once (clj->js props)))

(defn name-tag [email]
  (js/mixpanel.name_tag (clj->js email)))

(defn identify [uuid]
  (js/mixpanel.identify (clj->js uuid)))

(defn managed-track [event & [props]]
  (let [ch (chan)]
    (js/mixpanel.track event (clj->js props)
                       #(do (put! ch %) (close! ch)))
    ch))

(defn set-people-props [props]
  (js/mixpanel.people.set (clj->js props)))
