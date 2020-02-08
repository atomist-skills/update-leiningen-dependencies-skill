(ns atomist.leiningen
  (:require [atomist.lein]
            [cljs-node-io.core :as io]
            [cljs-node-io.fs :as fs]
            [atomist.cljs-log :as log]
            [atomist.sdmprojectmodel :as sdm]
            [cljs.core.async :refer [<! timeout]]
            [atomist.sha :as sha]
            [atomist.json :as json]
            [goog.string :as gstring]
            [goog.string.format])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def configs [{:name "metosin/compojure-api"
               :parameters [{:name "name" :value "metosin/compojure-api"}
                            {:name "version" :value "1.1.12"}]}
              {:name "crap1"
               :parameters [{:name "name" :value "crap1"}
                            {:name "version" :value "1.1"}]}])

(def configs1 [{:name "crap" :parameters nil}])

(defn- get-param [x s]
  (->> x (filter #(= s (:name %))) first :value))

(defn- target-map
  [configs]
  (->> configs
       (filter #(and (not (empty? (:parameters %)))))
       (map :parameters)
       (map (fn [x] (assoc {} :name (get-param x "name") :version (get-param x "version"))))
       (filter #(and (:name %) (:version %)))
       (map #(let [data [(:name %) (:version %)]] (assoc % :sha (sha/sha-256 (json/->str data)) :data data)))
       (map #(assoc % :name (gstring/replaceAll (:name %) "/" "::")
                      :library (:name %)))
       (into [])))

(defn- off-target? [fingerprint target]
  (and (= (:name fingerprint) (:name target))
       (not (= (:sha fingerprint) (:sha target)))))

(declare apply-library-editor)

(defn apply-leiningen-dependency
  "iterate over configuration targets and fingerprints and check for off-target fingerprints
    we wrap any edits in sdm/commit-then-PR so this function might create several PRs,
    depending on how many off target leiningen versions are present.  "
  [{:keys [project configurations fingerprints] :as request}]
  (log/info "configurations " configurations)
  (go
   (let [targets (target-map configurations)]
     (doseq [{current-data :data :as fingerprint} fingerprints]
       (doseq [{target-data :data :as target} targets]
         (when (off-target? fingerprint target)
           (let [body (gstring/format "off-target clojure-project-deps %s/%s -> %s/%s"
                                      (nth current-data 0) (nth current-data 1)
                                      (nth target-data 0) (nth target-data 1))]
             (log/info body)
             (<! (apply-library-editor project
                                       {:branch (:library target)
                                        :target-branch (-> request :ref :branch)
                                        :title (gstring/format "%s:  update leiningen dependencies skill requesting change" (:library target))
                                        :body body}
                                       (nth target-data 0)
                                       (nth target-data 1)))))))
     :complete)))

(defn extract
  "extract fingerprints from a project.clj file

    we use cljs-node-io Files when we call atomist.lein/deps

    returns array of leiningen fingerprints or empty [] if project.clj is not present"
  [project]
  (let [f (io/file (. ^js project -baseDir) "project.clj")]
    (if (fs/fexists? (.getPath f))
      (atomist.lein/deps f)
      [])))

(comment
 ;; this will actually raise a PR when run with a real project
 (let [project #js {:baseDir "/Users/slim/atomist/atomisthqa/clj1"}]
   (apply-leiningen-dependency {:fingerprints (extract project)
                                :project project
                                :ref {:branch "master"}
                                :configurations configs})))

(defn- apply-library-editor
  "apply a fingerprint inside of a PR

    params
      project - the SDM project
      f - this is cljs-node-io File, not an SDM File
      pr-opts - must conform to {:keys [branch target-branch title body]}
      library-name - leiningen library name string
      library-version - leiningen library version string

    returns channel"
  [project pr-opts library-name library-version]
  ((sdm/commit-then-PR
    (fn [p] (go
             (try
               (let [f (io/file (. ^js project -basedir) "project.clj")]
                 (io/spit f (atomist.lein/edit-library (io/slurp f) library-name library-version)))
               :success
               (catch :default ex
                 (log/error "failure updating project.clj for dependency change")
                 :failure))))
    pr-opts) project))