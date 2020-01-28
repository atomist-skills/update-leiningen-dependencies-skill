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
            [atomist.promise :as promise])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn send-message [request]
  (go
   (<! (api/simple-message request (gstring/format
                                    "Project %s/%s has %s files"
                                    (-> request :ref :owner)
                                    (-> request :ref :repo)
                                    (:results request))))
   (>! (:done-channel request) :done)))

(defn ^:export handler
  "handler
    must return a Promise - we don't do anything with the value
    params
      data - Incoming Request #js object
      sendreponse - callback ([obj]) puts an outgoing message on the response topic"
  [data sendreponse]
  (api/make-request data sendreponse
   (-> send-message
       (api/run-sdm-project-callback
        (fn [project] (go (<! (promise/from-promise (.totalFileCount project))))))
       (api/create-ref-from-first-linked-repo)
       (api/extract-linked-repos)
       (api/extract-github-user-token))))
