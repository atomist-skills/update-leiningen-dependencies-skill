(ns user
  (:require [atomist.main]
            [atomist.cljs-log :as log]
            [atomist.api :as api]
            [atomist.deps :as deps]
            [atomist.leiningen]
            [cljs.core.async :refer [<! chan]]
            [atomist.sdmprojectmodel :as sdm]
            [cljs-node-io.core :refer [slurp spit]]
            [atomist.editors :as editors]
            [atomist.cljs-log :as log]
            ["@atomist/automation-client" :as ac]
            ["@atomist/automation-client/lib/operations/support/editorUtils" :as editor-utils]
            [atomist.json :as json]
            [atomist.promise :as promise])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(comment

 ((api/run-sdm-project-callback
   (fn [request] (println "final callback" request))
   atomist.main/compute-fingerprints)
  {:ref {:branch "master"
         :owner "atomisthqa"
         :repo "clj1"}
   :token github-token
   :secrets [{:uri "atomist://api-key" :value token}]
   :team {:id "AK748NQC5"}
   :configurations [{:parameters [{:name "policy" :value "manualConfiguration"}
                                  {:name "dependencies" :value "[[org.clojure/clojure \"1.10.2\"]]"}]}
                    {:parameters [{:name "policy" :value "latestSemVerAvailable"}
                                  {:name "dependencies" :value "[mount]"}]}
                    {:parameters [{:name "policy" :value "latestSemVerUsed"}
                                  {:name "dependencies" :value "[com.atomist/common]"}]}]})

 ((api/run-sdm-project-callback
   (fn [request] (println "final callback" request))
   (fn [request project]
     ((sdm/edit-inside-PR
       (fn [p]
         ((sdm/do-with-files
           (fn [f]
             (go (<! (sdm/set-content f (str (<! (sdm/get-content f)) ".")))
                 true))
           "**/README.md") p))
       {:branch "testbranch"
        :target-branch "master"
        :body "atomist-determined body"
        :title "atomist-determined title"}) project)))
  {:ref {:branch "master"
         :owner "atomisthqa"
         :repo "clj1"}
   :token github-token
   :done-channel (chan)
   :sendreponse (fn [obj] (log/info "sendreponse " (js->clj obj)))
   :secrets [{:uri "atomist://api-key" :value token}]})

 (atomist.main/handler
  #js {:command "ShowLeiningenDependencies"
       :source {:slack {:channel {:id "CDU23TC1H"}
                        :user {:id "UDF0NFB5M"}}}
       :team {:id "AK748NQC5"}
       :parameters []
       :configurations [{:parameters [{:name "policy" :value "manualConfiguration"}
                                      {:name "dependencies" :value "[[org.clojure/clojure \"1.10.2\"]]"}]}
                        {:parameters [{:name "policy" :value "latestSemVerAvailable"}
                                      {:name "dependencies" :value "[mount]"}]}
                        {:parameters [{:name "policy" :value "latestSemVerUsed"}
                                      {:name "dependencies" :value "[com.atomist/common]"}]}]
       :raw_message "not used"
       :secrets [{:uri "atomist://api-key" :value token}]}
  fake-response)


 ;; this will actually raise a PR when run with a real project
 (let [project #js {:baseDir "/Users/slim/atomist/atomisthqa/clj1"}]
   (go (println (<!
                 (deps/apply-policy-target
                  {:fingerprints (atomist.leiningen/extract project)
                   :project project
                   :ref {:branch "master"}
                   :secrets [{:uri "atomist://api-key" :value token}]
                   :configurations [{:parameters [{:name "policy" :value "manualConfiguration"}
                                                  {:name "dependencies" :value "[[org.clojure/clojure \"1.10.1\"]]"}]}
                                    {:parameters [{:name "policy" :value "latestSemVerAvailable"}
                                                  {:name "dependencies" :value "[mount]"}]}
                                    {:parameters [{:name "policy" :value "latestSemVerUsed"}
                                                  {:name "dependencies" :value "[com.atomist/common]"}]}]}
                  atomist.leiningen/apply-library-editor))))))

(enable-console-print!)

(def token (.. js/process -env -API_KEY_SLIMSLENDERSLACKS_STAGING))
(def github-token (.. js/process -env -GITHUB_TOKEN))

(defn fake-response [obj]
  (log/info "response ---------------")
  (log/info (js->clj obj))
  (log/info "response ---------------"))

(sdm/enable-sdm-debug-logging)

(comment
 ;; response that indicates there is no linked credential
 (atomist.main/handler #js {:command "ShowLeiningenDependencies"
                            :source {:slack {:channel {:id "C19ALS7P1"}
                                             :user {:id "U09MZ63EW"}}}
                            :team {:id "AK748NQC5"}
                            :parameters []
                            :raw_message "update-leiningen-dependencies"
                            :secrets [{:uri "atomist://api-key" :value token}]}
                       (fn [& args] (log/info "sendreponse " args)))

 ;; should make it through and then find no fingerprints for this repo
 (atomist.main/handler #js {:command "ShowLeiningenDependencies"
                            :source {:slack {:channel {:id "CTGGW07B6"}
                                             :user {:id "UDF0NFB5M"}}}
                            :team {:id "AK748NQC5"}
                            :parameters []
                            :raw_message "update-leiningen-dependencies"
                            :secrets [{:uri "atomist://api-key" :value token}]}
                       (fn [& args] (log/info "sendreponse " args)))

 ;; switch to a clojure repo and see some actual fingerprints
 (atomist.main/handler #js {:command "ShowLeiningenDependencies"
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
                            :extensions {:team_id "AK748NQC5"}}
                       (fn [& args] (log/info "sendreponse " args)))

 ;; PUSH handler on the clj1 repo
 (atomist.main/handler #js {:data {:Push [{:branch "master"
                                           :repo {:name "clj1"
                                                  :org {:owner "atomisthqa"
                                                        :scmProvider {:providerId "zjlmxjzwhurspem"
                                                                      :credential {:secret github-token}}}}
                                           :after {:message ""}}]}
                            :secrets [{:uri "atomist://api-key" :value token}]
                            :configurations [{:parameters [{:name "policy" :value "manualConfiguration"}
                                                           {:name "dependencies" :value "[[org.clojure/clojure \"1.10.2\"]]"}]}
                                             {:parameters [{:name "policy" :value "latestSemVerAvailable"}
                                                           {:name "dependencies" :value "[\"mount\"]"}]}
                                             {:parameters [{:name "policy" :value "latestSemVerUsed"}
                                                           {:name "dependencies" :value "[\"com.atomist/common\"]"}]}]
                            :extensions {:team_id "AK748NQC5"}}
                       (fn [& args] (log/info "sendreponse " args)))

 (go
  (cljs.pprint/pprint (<! (sdm/do-with-shallow-cloned-project
                           (fn [p] (atomist.main/compute-fingerprints p))
                           github-token
                           {:owner "atomisthqa" :repo "clj1" :branch "master"}))))

 ((api/run-sdm-project-callback
   (fn [request]
     (cljs.pprint/pprint request))
   atomist.main/compute-fingerprints)
  {:ref {:owner "atomisthqa" :repo "clj1" :branch "master"}
   :token github-token})

 ;; get a Project by cloning
 (go (def p (<! (sdm/do-with-shallow-cloned-project
                 (fn [p]
                   (go p))
                 github-token
                 {:repo "elephants"
                  :owner "atomisthqa"
                  :branch "master"}))))

 ;; find a linked Slack Team
 (go (println (<! (api/linked-slack-team->channel {:secrets [{:uri "atomist://api-key" :value token}]
                                                   :team {:id "AK748NQC5"}}))))

 ;; test middleware for decorating a schedule event with a slack source from the graph
 ((api/add-slack-source-to-event (fn [request]
                                   (println "request " request)) :channel "thingy")
  {:secrets [{:uri "atomist://api-key" :value token}]
   :team {:id "AK748NQC5"}})

 )