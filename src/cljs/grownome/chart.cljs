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
  [top-level-labels name metric]
  (let [context (.getContext (.getElementById js/document (str "rev-chartjs-" name)) "2d")
        chart-data {:type "line"
                    :data {:labels top-level-labels
                           :datasets [{:label name
                                       :fill false
                                       :showLine true
                                       :borderJoinStyle "round"
                                       :borderWidth 0.5
                                       :radius 1
                                       :hitRadius 3
                                       :borderColor "#F08080"
                                       :tension 1
                                       :data metric}
                                      ]
                           }}]
      (js/Chart. context (clj->js chart-data))))

(defn chart
  [labels name metric]
  (reagent/create-class
    {:component-did-mount #(show-metrics-chart labels name metric)
     :display-name        (str "chartjs-component-" name)
     :reagent-render      (fn []
                            [:canvas
                             {:id (str "rev-chartjs-" name)
                              :width "700"
                              :height "380"}])}))
