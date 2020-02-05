(ns user
  (:require [atomist.main]
            [atomist.cljs-log :as log]))

(enable-console-print!)

(def token (.. js/process -env -API_KEY_SLIMSLENDERSLACKS_STAGING))
(def github-token (.. js/process -env -GITHUB_TOKEN))

(comment
 ;; response that indicates there is no linked credential
 (atomist.main/handler #js {:command "UpdateLeiningenDependencies"
                            :source {:slack {:channel {:id "C19ALS7P1"}
                                             :user {:id "U09MZ63EW"}}}
                            :team {:id "AK748NQC5"}
                            :parameters []
                            :raw_message "update-leiningen-dependencies"
                            :secrets [{:uri "atomist://api-key" :value token}]}
                       (fn [& args] (log/info "sendreponse " args)))

 ;; should make it through and then find no fingerprints for this repo
 (atomist.main/handler #js {:command "UpdateLeiningenDependencies"
                            :source {:slack {:channel {:id "CTGGW07B6"}
                                             :user {:id "UDF0NFB5M"}}}
                            :team {:id "AK748NQC5"}
                            :parameters []
                            :raw_message "update-leiningen-dependencies"
                            :secrets [{:uri "atomist://api-key" :value token}]}
                       (fn [& args] (log/info "sendreponse " args)))

 ;; switch to a clojure repo and see some actual fingerprints
 (atomist.main/handler #js {:command "UpdateLeiningenDependencies"
                            :source {:slack {:channel {:id "CDU23TC1H"}
                                             :user {:id "UDF0NFB5M"}}}
                            :team {:id "AK748NQC5"}
                            :parameters []
                            :raw_message "update-leiningen-dependencies"
                            :secrets [{:uri "atomist://api-key" :value token}]}
                       (fn [& args] (log/info "sendreponse " args)))

 ;; PUSH handler on a non clojure repo
 (atomist.main/handler #js {:data {:Push [{:branch "master"
                                           :repo {:name "elephants"
                                                  :org {:owner "atomisthqa"
                                                        :scmProvider {:providerId "zjlmxjzwhurspem"
                                                                      :credential {:secret github-token}}}}
                                           :after {:message ""}}]}
                            :secrets [{:uri "atomist://api-key" :value token}]
                            :configurations []
                            :extensions [:team_id "AK748NQC5"]}
                       (fn [& args] (log/info "sendreponse " args)))

 ;; PUSH handler on the clj1 repo
 (atomist.main/handler #js {:data {:Push [{:branch "master"
                                           :repo {:name "clj1"
                                                  :org {:owner "atomisthqa"
                                                        :scmProvider {:providerId "zjlmxjzwhurspem"
                                                                      :credential {:secret github-token}}}}
                                           :after {:message ""}}]}
                            :secrets [{:uri "atomist://api-key" :value token}]
                            :configurations []
                            :extensions [:team_id "AK748NQC5"]}
                       (fn [& args] (log/info "sendreponse " args)))
 )
