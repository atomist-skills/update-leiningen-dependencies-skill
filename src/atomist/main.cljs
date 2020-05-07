(ns atomist.main
  (:require [cljs.pprint :refer [pprint]]
            [cljs.core.async :refer [<! >! timeout chan]]
            [goog.string.format]
            [atomist.cljs-log :as log]
            [atomist.api :as api]
            [atomist.leiningen :as leiningen]
            [atomist.deps :as deps])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn just-fingerprints
  [_ project]
  (go
    (try
      (let [fingerprints (leiningen/extract project)]
       ;; return the fingerprints in a form that they can be added to the graph
        fingerprints)
      (catch :default ex
        (log/error "unable to compute leiningen fingerprints")
        (log/error ex)
        {:error ex
         :message "unable to compute leiningen fingerprints"}))))

(def apply-policy (partial deps/apply-policy-targets {:type "maven-direct-dep"
                                                      :apply-library-editor leiningen/apply-library-editor
                                                      :->library-version leiningen/data->library-version
                                                      :->data leiningen/library-version->data
                                                      :->sha leiningen/data->sha
                                                      :->name leiningen/library-name->name}))

(defn compute-fingerprints
  [request project]
  (go
    (try
      (let [fingerprints (leiningen/extract project)]
       ;; first create PRs for any off-target deps
        (<! (apply-policy
             (assoc request :project project :fingerprints fingerprints)))
       ;; return the fingerprints in a form that they can be added to the graph
        fingerprints)
      (catch :default ex
        (log/error "unable to compute leiningen fingerprints")
        (log/error ex)
        {:error ex
         :message "unable to compute leiningen fingerprints"}))))

(defn ^:export handler
  "handler
    must return a Promise - we don't do anything with the value
    params
      data - Incoming Request #js object
      sendreponse - callback ([obj]) puts an outgoing message on the response topic"
  [data sendreponse]
  (deps/deps-handler data
                     sendreponse
                     ["ShowLeiningenDependencies"]
                     ["SyncLeiningenDependency"]
                     ["UpdateLeiningenDependency"
                      (api/compose-middleware
                       [deps/set-up-target-configuration]
                       [api/check-required-parameters {:name "dependency"
                                                       :required true
                                                       :pattern ".*"
                                                       :validInput "[lib-symbol version]"}]
                       [api/extract-cli-parameters [[nil "--dependency dependency" "[lib version]"]]])]
                     just-fingerprints
                     compute-fingerprints
                     deps/mw-validate-policy))
