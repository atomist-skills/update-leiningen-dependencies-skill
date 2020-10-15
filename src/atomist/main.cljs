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

(ns atomist.main
  (:require [goog.string.format]
            [atomist.api :as api]
            [atomist.leiningen :as leiningen]
            [atomist.deps :as deps]))

(defn ^:export handler
  "handler
    must return a Promise - we don't do anything with the value
    params
      data - Incoming Request #js object
      sendreponse - callback ([obj]) puts an outgoing message on the response topic"
  [data sendreponse]
  (deps/deps-handler
   data
   sendreponse
   :deps-command/show "ShowLeiningenDependencies"
   :deps-command/sync "SyncLeiningenDependency"
   :deps-command/update "UpdateLeiningenDependency"
   :deps/type "maven-direct-dep"
   :deps/apply-library-editor leiningen/apply-library-editor
   :deps/extract leiningen/extract
   :deps/->library-version leiningen/data->library-version
   :deps/->data leiningen/library-version->data
   :deps/->sha leiningen/data->sha
   :deps/->name leiningen/library-name->name
   :deps/validate-policy deps/mw-validate-policy
   :deps/validate-command-parameters (api/compose-middleware
                                      [deps/set-up-target-configuration]
                                      [api/check-required-parameters {:name "dependency"
                                                                      :required true
                                                                      :pattern ".*"
                                                                      :validInput "[lib version]"}]
                                      [api/extract-cli-parameters [[nil "--dependency dependency" "[lib version]"]]])))
