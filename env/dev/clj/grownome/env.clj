(ns grownome.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [grownome.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[grownome started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[grownome has shut down successfully]=-"))
   :middleware wrap-dev})
