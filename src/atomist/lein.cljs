(ns atomist.lein
  (:require [rewrite-clj.zip :as z]
            [cljs-node-io.core :refer [slurp spit]]
            [goog.crypt :as crypt]
            [cljs.pprint :refer [pprint]]
            [cljs.reader :refer [read-string]]
            [goog.string :as gstring]
            [goog.string.format]
            [atomist.json :as json]
            [atomist.sha :as sha]))

(defn edit-library [s library-name library-version]
  (-> s
      (z/of-string)
      z/down
      (z/find-next-value :dependencies)
      (z/find z/next #(if-let [s (z/sexpr %)]
                        (and (symbol? s)
                             (= library-name (str s))
                             (= :vector (-> % z/prev z/node :tag)))))
      (z/right)
      (z/edit (constantly library-version))
      (z/root-string)))

(defn string->bytes [s]
  (crypt/stringToUtf8ByteArray s))

(defn digest [hasher bytes]
  (.update hasher bytes)
  (.digest hasher))

(defn bytes->hex
  "convert bytes to hex"
  [bytes-in]
  (crypt/byteArrayToHex bytes-in))

(defn sha-256 [s]
  (bytes->hex
   (digest (goog.crypt.Sha256.) (string->bytes s))))

(defn dependencies
  ([zipper]
   (-> zipper
       z/down
       (z/find-next-value :dependencies)
       z/right)))

(defn lein-deps [s]
  (->> s
       (z/of-string)
       dependencies
       (z/sexpr)
       (sort-by (comp name first))
       (map #(conj (rest %) (str (first %))))))

(defn project-dependencies [f]
  (lein-deps (slurp f)))

(defn get-version [f]
  (-> f
      (slurp)
      (read-string)
      (nth 2)))

(defn get-name [f]
  (-> f
      (slurp)
      (read-string)
      (nth 1)
      (str)))
