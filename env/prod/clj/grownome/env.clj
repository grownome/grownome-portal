(ns grownome.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[grownome started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[grownome has shut down successfully]=-"))
   :middleware identity})
