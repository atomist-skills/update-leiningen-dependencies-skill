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
