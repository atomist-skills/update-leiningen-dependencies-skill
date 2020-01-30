(ns atomist.leiningen
  (:require [atomist.lein]
            [cljs-node-io.core :as io]
            [cljs-node-io.fs :as fs]
            [atomist.cljs-log :as log]))

(defn apply-leiningen-dependency [project offTargets]
  (let [f (io/file (. project -basedir) "project.clj")]
    (if (fs/fexists? f)
      (doseq [fingerprint offTargets]
        (io/spit f (atomist.lein/edit-library (io/slurp f) (-> fingerprint :data (nth 0)) (-> fingerprint :data (nth 1))))))))
(defn extract [project]
  (let [f (io/file (. project -baseDir) "project.clj")]
    (if (fs/fexists? f)
      (atomist.lein/deps f)
      [])))
