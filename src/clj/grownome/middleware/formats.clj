(ns grownome.middleware.formats
  (:require [cognitect.transit :as transit]
            [luminus-transit.time :as time]
            [clj-time.coerce :as coerce]
            [muuntaja.format.transit :as formats]
            [muuntaja.core :as m])
  (:import [com.fasterxml.jackson.datatype.jdk8 Jdk8Module]
           [java.time LocalDateTime]))

(def joda-time-writer
  (transit/write-handler
   (constantly "m")
   (fn [v] (-> ^org.joda.time.ReadableInstant v .getMillis))
   (fn [v] (-> ^org.joda.time.ReadableInstant v .getMillis .toString))))


(def wrap-format-options
  )

(def instance
  (m/create
    (-> m/default-options
        (assoc-in
          [:formats "application/json" :opts :modules]
          [(Jdk8Module.)])
        (update-in
          [:formats "application/transit+json" :decoder-opts]
          (partial merge time/time-deserialization-handlers))
        (update-in

         [:formats "application/transit+json" ]
         (partial merge {:decoder [ (partial formats/decoder :json)]}))

        (update-in
         [:formats "application/transit+json" :encoder-opts]
         (partial merge time/time-serialization-handlers))

        (update
         :formats
         merge
         {"application/transit+json"
          {:decoder [(partial formats/decoder :json)]
           :encoder [#(formats/encoder
                       :json
                       (merge
                        %
                        {:handlers {org.joda.time.DateTime joda-time-writer}}))]}})
        )))

;; (update-in
;;  [:formats "application/transit+json" ]
;;  (partial merge {:encoder [#(formats/encoder
;;                              :json
;;                              (merge
;;                               %
;;                               {:handlers
;;                                {org.joda.time.DateTime joda-time-writer}}))]}))
