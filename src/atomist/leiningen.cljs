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

(ns atomist.leiningen
  (:require [cljs-node-io.core :as io]
            [atomist.cljs-log :as log]
            [clojure.string :as s]
            [atomist.sha :as sha]
            [atomist.json :as json]
            [goog.string :as gstring]
            [goog.string.format]
            [atomist.lein :as lein])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn library-name->name [s]
  (-> s
      (s/replace #"@" "")
      (s/replace #"/" "::")))

(defn data->sha [data]
  (sha/sha-256 (json/->str data)))

(defn data->library-version [data]
  [(if (= (:group data) (:artifact data))
     (:artifact data)
     (gstring/format "%s/%s" (:group data) (:artifact data)))
   (:version data)])

(defn library-version->data [[library version]]
  (assoc
   (if-let [[_ group artifact] (re-find #"(.*)/(.*)" library)]
     {:group group
      :artifact artifact}
     {:group library
      :artifact library})
   :version version))

(defn ->coordinate [[n v]]
  (merge
   {:version v}
   (if-let [[_ g a] (re-find #"(.*)/(.*)" n)]
     {:group g
      :artifact a}
     {:group n
      :artifact n})))

(defn deps
  " sha is checksum of jsonified 2-tuple array of [string library, string version] in lein format
    data is the jsonified 2-tuple array
    name is the string library dep but with the / replaced by ::"
  [f]
  (->> (for [dep (lein/project-dependencies f) :let [data (into [] (take 2 dep))]]
         {:type "maven-direct-dep"
          :name (library-name->name (nth dep 0))
          :displayName (nth dep 0)
          :displayValue (nth data 1)
          :displayType "MVN Coordinate"
          :data (->coordinate dep)
          :sha (data->sha (->coordinate dep))
          :abbreviation "m2"
          :version "0.0.1"})
       (into [])))

(defn extract
  "extract fingerprints from a project.clj file

    we use cljs-node-io Files when we call atomist.lein/deps

    returns array of leiningen fingerprints or empty [] if project.clj is not present"
  [project]
  (go
    (let [f (io/file (:path project) "project.clj")]
      (if (.exists f)
        (deps f)
        []))))

(defn apply-library-editor
  "apply a library edit inside of a PR

    params
      project - the SDM project
      f - this is cljs-node-io File, not an SDM File
      pr-opts - must conform to {:keys [branch target-branch title body]}
      library-name - leiningen library name string
      library-version - leiningen library version string

    returns channel"
  [project target-fingerprint]
  (go
    (try
      (let [f (io/file (:path project) "project.clj")
            [library-name library-version] (data->library-version (:data target-fingerprint))]
        (io/spit f (atomist.lein/edit-library (io/slurp f) library-name library-version)))
      :success
      (catch :default ex
        (log/error "failure updating project.clj for dependency change" ex)
        :failure))))

