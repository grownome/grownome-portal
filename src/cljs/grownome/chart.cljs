(ns grownome.chart
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [cljsjs.chartjs]))

(def data-set-example
  [{:data [5 10 15 20 25]
    :label "Rev in MM"
    :backgroundColor "#90EE90"}
   {:data [3 6 9 12 15]
    :label "Cost in MM"
    :backgroundColor "#F08080"}])
(def lable-example
  ["2012" "2013" "2014" "2015" "2016"])


(defn show-metrics-chart
  [top-level-labels humidity temp]
  (let [context (.getContext (.getElementById js/document "rev-chartjs") "2d")
        chart-data {:type "line"
                    :data {:labels top-level-labels
                           :datasets [{:label "humidity"
                                       :fill false
                                       :showLine true
                                       :borderColor "#F08080"
                                       :data humidity}
                                      {:label "temperature"
                                       :fill false
                                       :showLine true
                                       :borderColor "#00F0F0"
                                       :data temp}
                                      ]}}]
      (js/Chart. context (clj->js chart-data))))

(defn chart
  [labels humidity temp]
  (reagent/create-class
    {:component-did-mount #(show-metrics-chart labels humidity temp)
     :display-name        "chartjs-component"
     :reagent-render      (fn []
                            [:canvas {:id "rev-chartjs" :width "700" :height "380"}])})) 
