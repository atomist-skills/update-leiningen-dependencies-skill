(ns atomist.main
  (:require [cljs.pprint :refer [pprint]]
            [cljs.core.async :refer [<! >! timeout chan] :as async]
            [clojure.string :as s]
            [goog.crypt.base64 :as b64]
            [goog.string :as gstring]
            [goog.string.format]
            [atomist.cljs-log :as log]
            [atomist.editors :as editors]
            [atomist.sdmprojectmodel :as sdm]
            [atomist.json :as json]
            [atomist.api :as api]
            [atomist.promise :as promise]
            [atomist.leiningen :as leiningen]
            [atomist.sha :as sha]
            [cljs-node-io.core :as io]
            ["@atomist/automation-client" :as ac]
            [atomist.deps :as deps])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn just-fingerprints
  [request project]
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

(defn compute-fingerprints
  [request project]
  (go
   (try
     (let [fingerprints (leiningen/extract project)]
       ;; first create PRs for any off target deps
       (<! (deps/apply-policy-targets
            (assoc request :project project :fingerprints fingerprints)
            "clojure-project-deps"
            leiningen/apply-library-editor))
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
  (deps/deps-handler data sendreponse
                     ["ShowLeiningenDependencies"
                      just-fingerprints]
                     ["UpdateLeiningenDependency"
                      compute-fingerprints
                      (api/compose-middleware
                       [deps/set-up-target-configuration]
                       [api/check-required-parameters {:name "dependency"
                                                       :required true
                                                       :pattern ".*"
                                                       :validInput "[lib-symbol version]"}]
                       [api/extract-cli-parameters [[nil "--dependency dependency" "[lib version]"]]])]
                     deps/mw-validate-policy))
