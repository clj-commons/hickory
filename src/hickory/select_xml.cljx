(ns hickory.select-xml
  "Functions to query hickory-format XML data.

   See clojure.zip for more information on zippers, locs, nodes, next, etc."
  (:require [clojure.zip :as zip]))


(defn attr
  "Selector which provides case sensitive version of (hickory.select/attr)"
  ([attr-name]
   (attr attr-name (fn [_] true)))
  ([attr-name predicate]
   (fn [hzip-loc]
     (let [node (zip/node hzip-loc)
           attr-key (keyword (name attr-name))]
       (if (clojure.core/and (contains? (:attrs node) attr-key)
                             (predicate (get-in node [:attrs attr-key])))
         hzip-loc)))))
