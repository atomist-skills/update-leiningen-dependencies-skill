(ns atomist.leiningen
  (:require [atomist.lein]
            [cljs-node-io.core :as io]
            [cljs-node-io.fs :as fs]
            [atomist.cljs-log :as log]
            [atomist.sdmprojectmodel :as sdm]
            [cljs.core.async :refer [<! timeout chan]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn extract
  "extract fingerprints from a project.clj file

    we use cljs-node-io Files when we call atomist.lein/deps

    returns array of leiningen fingerprints or empty [] if project.clj is not present"
  [project]
  (let [f (io/file (. ^js project -baseDir) "project.clj")]
    (if (fs/fexists? (.getPath f))
      (atomist.lein/deps f)
      [])))

(defn- apply-library-editor
  "apply a library edit inside of a PR

    params
      project - the SDM project
      f - this is cljs-node-io File, not an SDM File
      pr-opts - must conform to {:keys [branch target-branch title body]}
      library-name - leiningen library name string
      library-version - leiningen library version string

    returns channel"
  [project pr-opts library-name library-version]
  ((sdm/commit-then-PR
    (fn [p]
      (go
       (try
         (let [f (io/file (. ^js p -baseDir) "project.clj")]
           (io/spit f (atomist.lein/edit-library (io/slurp f) library-name library-version)))
         :success
         (catch :default ex
           (log/error "failure updating project.clj for dependency change" ex)
           :failure))))
    pr-opts) project))

