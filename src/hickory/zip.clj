(ns hickory.zip
  (:require [clojure.zip :as zip]))

;;
;; Hickory
;;

(defn hickory-zip
  "Returns a zipper for html dom maps (as from as-hickory),
  given a root element."
  [root]
  (zip/zipper (complement string?)
              (comp seq :content)
              (fn [node children]
                (assoc node :content (and children (apply vector children))))
              root))

;;
;; Hiccup
;;

(defn hiccup-zip
  "Returns a zipper for Hiccup forms, given a root form."
  [root]
  (let [children-pos #(if (map? (second %)) 2 1)]
    (zip/zipper
      vector?
      #(drop (children-pos %) %) ; get children
      #(into [] (concat (take (children-pos %1) %1) %2)) ; make new node
      root)))