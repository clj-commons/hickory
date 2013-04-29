(ns hickory.select
  "Functions to query hickory-format HTML data."
  (:require [clojure.zip :as zip]
            [clojure.string :as string]
            [hickory.zip :as hzip]))


;;
;; Select
;;

(defn select-locs
  "Given a selector function and a hickory data structure, returns a vector
   containing all of the zipper locs selected by the selector function."
  [selector-fn hickory-tree]
  (loop [loc (hzip/hickory-zip hickory-tree)
         selected-nodes (transient [])]
    (if (zip/end? loc)
      (persistent! selected-nodes)
      (recur (zip/next loc)
             (if (selector-fn loc)
               (conj! selected-nodes loc)
               selected-nodes)))))

(defn select
  "Given a selector function and a hickory data structure, returns a vector
   containing all of the hickory nodes selected by the selector function."
  [selector-fn hickory-tree]
  (mapv zip/node (select-locs selector-fn hickory-tree)))

;;
;; Selectors
;;

(defn has-class?
  "Returns a function that takes a zip-loc argument and returns the
  zip-loc passed in iff it has the given class. The class name comparison
  is done case-insensitively."
  [class-name]
  (letfn [(parse-classes [class-str]
            (into #{} (mapv string/lower-case
                            (string/split class-str #" "))))]
    (fn [zip-loc]
      (let [node (zip/node zip-loc)
            class-str (-> node :attrs :class)
            ;; Check first, since not all nodes will have :attrs key
            classes (if class-str
                      (parse-classes class-str))]
        (if (contains? classes (string/lower-case class-name))
          zip-loc)))))
