(ns atomist.lein-t
  (:require [cljs.test :refer-macros [deftest is]]
            [atomist.lein :as lein]))

(deftest edit-library-t
  (let [lein-project "(defproject :dependencies [[org/hello \"1.0\"]])"]
    (is (= "(defproject :dependencies [[org/hello \"1.1\"]])"
           (lein/edit-library lein-project "org/hello" "1.1")))
    (is (thrown?
         :default
         (lein/edit-library lein-project "org/hell" "1.1")))))

(deftest lein-deps-t
  (let [lein-project "(defproject :dependencies [[org/hello \"1.0\"] [a/b \"11.11.11\"]])"
        deps (lein/lein-deps lein-project)]
    (is (and
         (some #{'("org/hello" "1.0")} deps)
         (some #{'("a/b" "11.11.11")} deps)))))
