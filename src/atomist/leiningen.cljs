(ns atomist.leiningen
  (:require [atomist.lein]
            [cljs-node-io.core :as io]))

(defn apply-leiningen-dependency [project offTargets]
  (let [f (io/file (. project -basedir) "project.clj")]
    (doseq [fingerprint offTargets]
      (io/spit f (atomist.lein/edit-library (io/slurp f) (-> fingerprint :data (nth 0)) (-> fingerprint :data (nth 1)))))))
(defn extract [project]
  (atomist.lein/deps (io/file (. project -basedir) "project.clj")))
