(ns hickory.select
  "Functions to query hickory-format HTML data.

   See clojure.zip for more information on zippers, locs, nodes, next, etc."
  (:require [clojure.zip :as zip]
            [clojure.string :as string]
            [hickory.zip :as hzip])
  (:refer-clojure :exclude [class]))


;;
;; Select
;;

(defn select-next-loc
  "Given a selector function and a loc inside a hickory zip data structure,
   returns the next zipper loc that satisfies the selection function. This can
   be the loc that is passed in, so be sure to move to the next loc if you
   want to use this function to exhaustively search through a tree manually.
   Note that if there is no next node that satisfies the selection function, nil
   is returned."
  [selector-fn hzip-loc]
  (loop [loc hzip-loc]
    (if (zip/end? loc)
      nil
      (if (selector-fn loc)
        loc
        (recur (zip/next loc))))))

(defn select-locs
  "Given a selector function and a hickory data structure, returns a vector
   containing all of the zipper locs selected by the selector function."
  [selector-fn hickory-tree]
  (loop [loc (select-next-loc selector-fn
                              (hzip/hickory-zip hickory-tree))
         selected-nodes (transient [])]
    (if (nil? loc)
      (persistent! selected-nodes)
      (recur (select-next-loc selector-fn (zip/next loc))
             (conj! selected-nodes loc)))))

(defn select
  "Given a selector function and a hickory data structure, returns a vector
   containing all of the hickory nodes selected by the selector function."
  [selector-fn hickory-tree]
  (mapv zip/node (select-locs selector-fn hickory-tree)))

;;
;; Selectors
;;

(defn tag
  "Return a function that takes a zip-loc argument and returns the
   zip-loc passed in iff it has the given tag. The tag argument can be
   a String or Named (keyword, symbol). The tag name comparison
   is done case-insensitively."
  [tag]
  (fn [hzip-loc]
    (let [node (zip/node hzip-loc)
          node-tag (-> node :tag)]
      (if (and node-tag
               (= (string/lower-case (name node-tag))
                  (string/lower-case (name tag))))
        hzip-loc))))

(defn id
  "Returns a function that takes a zip-loc argument and returns the
   zip-loc passed in iff it has the given id. The id argument can be
   a String or Named (keyword, symbol). The id name comparison
   is done case-insensitively."
  [id]
  (fn [hzip-loc]
    (let [node (zip/node hzip-loc)
          id-str (-> node :attrs :id)]
      (if (and id-str
               (= (string/lower-case id-str)
                  (string/lower-case (name id))))
        hzip-loc))))

(defn class
  "Returns a function that takes a zip-loc argument and returns the
  zip-loc passed in iff it has the given class. The class argument can
  be a String or Named (keyword, symbol). The class name comparison
  is done case-insensitively."
  [class-name]
  (letfn [(parse-classes [class-str]
            (into #{} (mapv string/lower-case
                            (string/split class-str #" "))))]
    (fn [hzip-loc]
      (let [node (zip/node hzip-loc)
            class-str (-> node :attrs :class)
            ;; Check first, since not all nodes will have :attrs key
            classes (if class-str
                      (parse-classes class-str))]
        (if (contains? classes (string/lower-case (name class-name)))
          hzip-loc)))))

(defn attr
  ([attr-name]
     ;; Since we want this call to succeed in any case where this attr
     ;; is present, we pass in a function that always returns true.
     (attr attr-name (fn [_] true)))
  ([attr-name predicate]
     (fn [hzip-loc]
       (let [node (zip/node hzip-loc)
             attr-key (keyword (string/lower-case (name attr-name)))]
         ;; If the attribute does not exist, we'll return null. Otherwise,
         ;; we'll ask the predicate if we should return this hzip-loc.
         (if (and (contains? (:attrs node) attr-key)
                  (predicate (get-in node [:attrs attr-key])))
           hzip-loc)))))
