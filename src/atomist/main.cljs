(ns atomist.main
  (:require [cljs.pprint :refer [pprint]]
            [cljs.core.async :refer [<! >! timeout chan]]
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
            [atomist.sha :as sha])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn compute-leiningen-fingerprints
  "TODO - we used to support multiple pom.xml files in the Project.  The
          path field was added to the Fingerprint to manage this.
          This version supports only a pom.xml in the root of the Repo.

   Transform Maven dependencies into Fingerprints

   Our data structure has {:keys [group artifact version name version scope]}"
  [project]
  (go
   (let [fingerprints (leiningen/extract project)]
     (->> (for [x fingerprints]
            (assoc x
              :sha (sha/sha-256 (json/->str (:data x)))
              :value (json/->str (:value x))
              :displayName (:name x)
              :displayValue (nth (:data x) 1)
              :displayType "Lein dependencies"))
          (into [])))))

(defn check-for-targets-to-apply [handler]
  (fn [request]
    (if (not (empty? (-> request :data :CommitFingerprintImpact :offTarget)))
      (handler request)
      (go (>! (:done-channel request) :done)))))

(defn- handle-push-event [request]
  ((-> (fn [ch-request]
         (log/info "----> finished handling Push")
         (go (>! (:done-channel ch-request) :done)))
       (api/send-fingerprints)
       (api/run-sdm-project-callback compute-leiningen-fingerprints)
       (api/extract-github-token)
       (api/create-ref-from-push-event)) request))

(defn- handle-impact-event [request]
  ((-> (fn [ch-request]
         (log/info "----> finished handling CommitFingerprintImpact")
         (go (>! (:done-channel ch-request) :done)))
       (api/run-sdm-project-callback
        (sdm/commit-then-PR
         (fn [p] (leiningen/apply-leiningen-dependency p (-> request :data :CommitFingerprintImpact :offTarget)))
         {:branch (str (random-uuid))
          :target-branch "master"
          :body "apply maven target dependencies"
          :title "apply maven target dependencies"}))
       (api/extract-github-token)
       (api/create-ref-from-repo
        (-> request :data :CommitFingerprintImpact :repo)
        (-> request :data :CommitFingerprintImpact :branch))
       (check-for-targets-to-apply)) request))

(defn ^:export handler
  "handler
    must return a Promise - we don't do anything with the value
    params
      data - Incoming Request #js object
      sendreponse - callback ([obj]) puts an outgoing message on the response topic"
  [data sendreponse]
  (api/make-request
   data
   sendreponse
   (fn [request]
     (cond
       ;; handle Push events
       (= :Push (:data request))
       (handle-push-event request)
       ;; handle Commit Fingeprint Impact events
       (= :CommitFingerprintImpact (:data request))
       (handle-impact-event request)))))
