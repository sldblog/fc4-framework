(ns fc4c.io-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest is]]
            [fc4c.io :as io]
            [fc4c.model :as m]
            [fc4c.view :as v]))

(deftest read-mode
  (is (s/valid? ::m/model (io/read-model "test/data/model"))))

(deftest read-view
  (is (s/valid? ::v/view (io/read-view "test/data/views/middle.yaml"))))
