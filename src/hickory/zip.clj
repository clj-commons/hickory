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

;; Just to make things easier, we go ahead and do the work here to
;; make hiccup zippers work on both normalized (all items have tag,
;; attrs map, and any children) and unnormalized hiccup forms.

(defn- children
  "Takes a hiccup node (normalized or not) and returns its children nodes."
  [node]
  (if (vector? node)
    ;; It's a hiccup node vector.
    (if (map? (second node)) ;; There is an attr map in second slot.
      (seq (subvec node 2))  ;; So skip tag and attr vec.
      (seq (subvec node 1))) ;; Otherwise, just skip tag.
    ;; Otherwise, must have a been a node list
    node))

;; Note, it's not made clear at all in the docs for clojure.zip, but as far as
;; I can tell, you are given a node potentially with existing children and
;; the sequence of children that should totally replace the existing children.
(defn- make
  "Takes a hiccup node (normalized or not) and a sequence of children nodes,
   and returns a new node that has the the children argument as its children."
  [node children]
  ;; The node might be either a vector (hiccup form) or a seq (which is like a
  ;; node-list).
  (if (vector? node)
    (if (map? (second node))                 ;; Again, check for normalized vec.
      (into (subvec node 0 2) children)      ;; Attach children after tag&attrs.
      (apply vector (first node) children))  ;; Otherwise, attach after tag.
    children))   ;; We were given a list for node, so just return the new list.


(defn hiccup-zip
  "Returns a zipper for Hiccup forms, given a root form."
  [root]
  (zip/zipper sequential?
              children
              make
              root))
