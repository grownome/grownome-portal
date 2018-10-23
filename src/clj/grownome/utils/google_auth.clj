(ns grownome.utils.google-auth
  (:require [clj-http.client :as client]
            [clojure.data.codec.base64 :refer [encode]]
            )
  (:import [com.google.auth.oauth2.GoogleCredentials]))


(defn predict-request
  [image-bytes]
  {:payload
   {:image
    {:imageBytes image-bytes}}})

(defn get-image-prediction
  [image-url]
  (let [
        appCreds (com.google.auth.oauth2.GoogleCredentials/getApplicationDefault)
        image-bytes (encode (.getBytes (:body (client/get image-url))))
        request (predict-request image-bytes)]
    (.refreshIfExpired appCreds)
    (:body
     (client/post
      "https://automl.googleapis.com/v1beta1/projects/grownome/locations/us-central1/models/ICN2981125424368801813:predict"
      {:headers {
                 "Content-Type" "Application/json"
                 "Authorization"
                 (str "Bearer " (.getTokenValue (.getAccessToken appCreds)))}
       :body (str "{\"payload\": {\"image\": {\"imageBytes\":\"" (String. image-bytes) "\"}}}")}))))

