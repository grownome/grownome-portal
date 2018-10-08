(ns user
  (:require [grownome.config :refer [env]]
            [clojure.spec.alpha :as s]
            [expound.alpha :as expound]
            [mount.core :as mount]
            [grownome.figwheel :refer [start-fw stop-fw cljs]]
            [grownome.core :refer [start-app]]
            [grownome.db.core]
            [conman.core :as conman]
            [luminus-migrations.core :as migrations]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(defn start []
  (mount/start-without #'grownome.core/repl-server))

(defn stop []
  (mount/stop-except #'grownome.core/repl-server))

(defn restart []
  (stop)
  (start))

(defn restart-db []
  (mount/stop #'grownome.db.core/*db*)
  (mount/start #'grownome.db.core/*db*)
  (binding [*ns* 'grownome.db.core]
    (conman/bind-connection grownome.db.core/*db* "sql/queries.sql")))

(defn reset-db []
  (migrations/migrate ["reset"] (select-keys env [:database-url])))

(defn migrate []
  (migrations/migrate ["migrate"] (select-keys env [:database-url])))

(defn rollback []
  (migrations/migrate ["rollback"] (select-keys env [:database-url])))

(defn create-migration [name]
  (migrations/create name (select-keys env [:database-url])))


