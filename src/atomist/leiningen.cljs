(ns atomist.leiningen
  (:require [atomist.lein]
            [cljs-node-io.core :as io]
            [cljs-node-io.fs :as fs]
            [atomist.cljs-log :as log]))

;; TODO should return a channel
(defn apply-leiningen-dependency [{:keys [project data configurations]}]
  (log/info "configurations " configurations)
  (let [f (io/file (. ^js project -basedir) "project.clj")]
    (if (fs/fexists? (.getPath f))
      (doseq [{:keys [type name data] :as fingerprint} (-> data :CommitFingerprintImpact :offTarget)]
        (log/infof "offTarget %s %s %s" type name (str data))
        (if (= type "clojure-project-deps")
          (log/infof "clojure-project-deps %s %s" (nth data 0) (nth data 1))
          #_(io/spit f (atomist.lein/edit-library (io/slurp f) (-> data (nth 0)) (-> data (nth 1)))))))))

(defn extract [project]
  (let [f (io/file (. ^js project -baseDir) "project.clj")]
    (if (fs/fexists? (.getPath f))
      (atomist.lein/deps f)
      [])))
