(ns grownome.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [grownome.core-test]))

(doo-tests 'grownome.core-test)

