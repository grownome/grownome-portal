(ns grownome.validation
  (:require
   #?(:clj [clojure.spec.alpha :as s]
      :cljs [cljs.spec.alpha :as s])))

(s/def :device/id         integer?)
(s/def :device/name       string?)
(s/def :device/registry-id       string?)
(s/def :device/resin-name string?)
(s/def :device/short-link uri?)
(s/def :device/created-on inst?)
(s/def :grownome/device (s/keys :req-un [:device/id
                                         :device/name
                                         :device/resin-name
                                         :device/short-link
                                         :device/created-on]))




(s/def :image/md5 string?)
(s/def :image/path string?)
(s/def :image/created-on inst?)
(s/def :grownome/image (s/keys :req-un
                               [:image/md5
                                :device/id
                                :image/path
                                :image/created-on]))


(s/def :metric/timestamp inst?)
(s/def :metric/core-temp-max  (s/and float? #(< % 300)))
(s/def :metric/core-temp-main (s/and float? #(< % 300)))
(s/def :metric/humidity       (s/and float? #(> % 0)))
(s/def :metric/temperature    (s/and float? #(> % 0)))
(s/def :metric/device-id  :device/id)
(s/def :metric/metric-name     #{"humidity" "temperature"
                                 "core-temp-main" "core-temp-max"})

(s/def :grownome/metric (s/keys :req-un [:metric/device-id
                                         :metric/metric-name
                                         :metric/timestamp]
                                :opt-un [:metric/core-temp-main
                                         :metric/core-temp-max
                                         :metric/humidity
                                         :metric/temperature]))

(defn email?
  [s]
  #?(:clj true))


(s/def :user/id (s/and  string? #(< (count %) 50)))
(s/def :user/name string?)
(s/def :user/email (s/and string? email?))
(s/def :user/created-on inst?)
(s/def :grownome/user (s/keys :req [:user/id
                                    :user/name
                                    :user/email
                                    :user/created-on]))



(s/def :owner/id integer?)
(s/def :owner/created-on inst?)
(s/def :owner/device-id :device/id)
(s/def :owner/user-id   :user/id)
(s/def :grownome/owner (s/keys :req [:owner/device-id
                                     :owner/user-id
                                     :owner/created-on]
                               :opt [:owner/id]))





(s/def :alert/device-id :device/id)
(s/def :alert/created-on inst?)
(s/def :alert/user-id :user/id)
(s/def :alert/threshold (s/and float? #(> % 0) #(< % 400)))
(s/def :alert/above boolean?)
(s/def :alert/below boolean?)
(s/def :alert/phone-number string?)
(s/def :alert/description string?)

(s/def :grownome/alert
  (s/keys :req-un
          [:alert/device-id
           :alert/created-on
           :alert/user-id
           :alert/threshold
           :alert/above
           :alert/below
           :alert/phone-number
           :alert/description]))

