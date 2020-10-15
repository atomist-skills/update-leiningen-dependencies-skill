;; Copyright © 2020 Atomist, Inc.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

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
