(ns grownome.utils.google-auth
  (:require [clj-http.client :as client]
            [clojure.data.codec.base64 :refer [encode]]
            [clojure.java [io :as io]]
            [cheshire.core :as json])
  (:import
   [com.google.protobuf  ByteString]
   [com.google.cloud.automl.v1beta1
    ModelName
    Image
    ExamplePayload
    PredictionServiceClient]))

(defn predict-request
  [image-bytes]
  {:payload
   {:image
    {:imageBytes image-bytes}}})

(defn get-image-prediction
  [image-url]
  (let [model   (ModelName/of "grownome" "us-central1" "ICN2981125424368801813")
        predictor (PredictionServiceClient/create)
        image-bytes  (.build
                      (.setImageBytes
                       (Image/newBuilder)
                       (ByteString/copyFrom (:body  (client/get image-url {:as :byte-array})))))
        payload (.build
                 (.setImage
                  (ExamplePayload/newBuilder)
                  image-bytes))
        resp (.getPayloadList (.predict predictor
                                        ^ModelName model
                                        ^ExamplePayload payload
                                        ^java.utilMap (java.util.HashMap.)))
        labels (map (fn [annotation]
                      {:label (.getDisplayName annotation)
                       :score (.getScore (.getClassification annotation))}) resp)]
    {:url image-url
     :labels labels}))
