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

(ns atomist.main-t
  (:require [cljs.test :refer-macros [async deftest is testing run-tests]]
            [cljs.core.async :refer [>! <! timeout chan]]
            [atomist.promise :as promise]
            [atomist.api :as api]
            [atomist.cljs-log :as log])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(deftest invoke-handler-chain-test
  (let [event #js {:data {:Push []}
                   :extensions {:correlation_id "corrid"
                                :team_id "teamid"
                                :team_name "teamname"}}
        send-response-callback (fn [& args]
                                        ;; TODO can make assertions on messages sent to the response topic
                                 (is true)
                                 (new js/Promise (fn [resolver _] (resolver true))))
        request-handler-chain (-> (fn [request]
                                    (log/info "request " request)
                                    (is true)
                                    (go request))
                                  (api/status))]
    (async
     done
     (go
       (<! (promise/from-promise
            (api/make-request event send-response-callback request-handler-chain)))
       (log/info "okay done now")
       (done)))))
