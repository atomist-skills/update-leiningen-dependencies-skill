(ns user
  (:require [atomist.main]
            [atomist.cljs-log :as log]))

(enable-console-print!)

(def token (.. js/process -env -API_KEY_SLIMSLENDERSLACKS_PROD))

(comment
 (atomist.main/handler #js {:command "UpdateLeiningenDependencies"
                            :source {:slack {:channel {:id "C19ALS7P1"}
                                             :user {:id "U09MZ63EW"}}}
                            :team {:id "T095SFFBK"}
                            :parameters []
                            :raw_message "update-leiningen-dependencies"
                            :secrets [{:uri "atomist://api-key" :value token}]}
                       (fn [& args] (log/info "sendreponse " args))))
