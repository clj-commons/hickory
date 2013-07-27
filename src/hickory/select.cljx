(ns hickory.select
  "Functions to query hickory-format HTML data.

   See clojure.zip for more information on zippers, locs, nodes, next, etc."
  (:require [clojure.zip :as zip]
            [clojure.string :as string]
            [hickory.zip :as hzip])
  #+clj (:import clojure.lang.IFn)
  (:refer-clojure :exclude [and or not class]))

;;
;; Utilities
;;

(defn until
  "Calls f on val until pred called on the result is true. If not, it
   repeats by calling f on the result, etc. The value that made pred
   return true is returned."
  [f val pred]
  (let [next-val (f val)]
    (if (pred next-val)
      next-val
      (recur f next-val pred))))

(defn count-until
  "Calls f on val until pred called on the result is true. If not, it
   repeats by calling f on the result, etc. The count of times this
   process was repeated until pred returned true is returned."
  [f val pred]
  (loop [next-val val
         cnt 0]
    (if (pred next-val)
      cnt
      (recur (f next-val) (inc cnt)))))

(defn next-pred
  "Like clojure.zip/next, but moves until it reaches a node that returns
   true when the function in the pred argument is called on them, or reaches
   the end."
  [hzip-loc pred]
  (until zip/next hzip-loc #(clojure.core/or (zip/end? %)
                                     (pred %))))

(defn prev-pred
  "Like clojure.zip/prev, but moves until it reaches a node that returns
   true when the function in the pred argument is called on them, or reaches
   the beginning."
  [hzip-loc pred]
  (until zip/prev hzip-loc #(clojure.core/or (nil? %)
                                     (pred %))))

(defn left-pred
  "Like clojure.zip/left, but moves until it reaches a node that returns
   true when the function in the pred argument is called on them, or reaches
   the left boundary of the current group of siblings."
  [hzip-loc pred]
  (until zip/left hzip-loc #(clojure.core/or (nil? %)
                                     (pred %))))

(defn right-pred
  "Like clojure.zip/right, but moves until it reaches a node that returns
   true when the function in the pred argument is called on them, or reaches
   the right boundary of the current group of siblings."
  [hzip-loc pred]
  (until zip/right hzip-loc #(clojure.core/or (nil? %)
                                      (pred %))))

(defn next-of-node-type
  "Like clojure.zip/next, but only counts moves to nodes that have
   the given type."
  [hzip-loc node-type]
  (next-pred hzip-loc #(= node-type (:type (zip/node %)))))

(defn prev-of-node-type
  "Like clojure.zip/prev, but only counts moves to nodes that have
   the given type."
  [hzip-loc node-type]
  (prev-pred hzip-loc #(= node-type (:type (zip/node %)))))

(defn left-of-node-type
  "Like clojure.zip/left, but only counts moves to nodes that have
   the given type."
  [hzip-loc node-type]
  (left-pred hzip-loc #(= node-type (:type (zip/node %)))))

(defn right-of-node-type
  "Like clojure.zip/right, but only counts moves to nodes that have
   the given type."
  [hzip-loc node-type]
  (right-pred hzip-loc #(= node-type (:type (zip/node %)))))


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
;; Mostly based off the spec at http://www.w3.org/TR/selectors/#selectors
;; Some selectors are simply not possible outside a browser (active,
;; visited, etc).
;;

(defn node-type
  "Return a function that takes a zip-loc argument and returns the
   zip-loc passed in iff it has the given node type. The type
   argument can be a String or Named (keyword, symbol). The node type
   comparison is done case-insensitively."
  [type]
  (fn [hzip-loc]
    (let [node (zip/node hzip-loc)
          node-type (-> node :type)]
      (if (clojure.core/and node-type
                    (= (string/lower-case (name node-type))
                       (string/lower-case (name type))))
        hzip-loc))))

(defn tag
  "Return a function that takes a zip-loc argument and returns the
   zip-loc passed in iff it has the given tag. The tag argument can be
   a String or Named (keyword, symbol). The tag name comparison
   is done case-insensitively."
  [tag]
  (fn [hzip-loc]
    (let [node (zip/node hzip-loc)
          node-tag (-> node :tag)]
      (if (clojure.core/and node-tag
                    (= (string/lower-case (name node-tag))
                       (string/lower-case (name tag))))
        hzip-loc))))

(defn attr
  "Returns a function that takes a zip-loc argument and returns the
   zip-loc passed in iff it has the given attribute, and that attribute
   optionally satisfies a predicate given as an additional argument. With
   a single argument, the attribute name (a string, keyword, or symbol),
   the function returned will return the zip-loc if that attribute is
   present (and has any value) on the zip-loc's node. The attribute name
   will be compared case-insensitively, but the attribute value (if present),
   will be passed as-is to the predicate.

   If the predicate argument is given, it will only return the zip-loc if
   that predicate is satisfied when given the attribute's value as its only
   argument. Note that the predicate only gets called when the attribute is
   present, so it can assume its argument is not nil."
  ([attr-name]
     ;; Since we want this call to succeed in any case where this attr
     ;; is present, we pass in a function that always returns true.
     (attr attr-name (fn [_] true)))
  ([attr-name predicate]
     ;; Note that attribute names are normalized to lowercase by
     ;; jsoup, as an html5 parser should; see here:
     ;; http://www.whatwg.org/specs/web-apps/current-work/#attribute-name-state
     (fn [hzip-loc]
       (let [node (zip/node hzip-loc)
             attr-key (keyword (string/lower-case (name attr-name)))]
         ;; If the attribute does not exist, we'll definitely return null.
         ;; Otherwise, we'll ask the predicate if we should return hzip-loc.
         (if (clojure.core/and (contains? (:attrs node) attr-key)
                  (predicate (get-in node [:attrs attr-key])))
           hzip-loc)))))

(defn id
  "Returns a function that takes a zip-loc argument and returns the
   zip-loc passed in iff it has the given id. The id argument can be
   a String or Named (keyword, symbol). The id name comparison
   is done case-insensitively."
  [id]
  (attr :id #(= (string/lower-case %)
                (string/lower-case (name id)))))

(defn class
  "Returns a function that takes a zip-loc argument and returns the
   zip-loc passed in iff it has the given class. The class argument can
   be a String or Named (keyword, symbol). The class name comparison
   is done case-insensitively."
  [class-name]
  (letfn [(parse-classes [class-str]
                       (into #{} (mapv string/lower-case
                                       (string/split class-str #" "))))]
    (attr :class #(contains? (parse-classes %)
                             (string/lower-case (name class-name))))))

(defn any
  "This selector takes no args, it simply is the selector function. It returns
   true on any element it is called on; corresponds to the CSS '*' selector."
  [hzip-loc]
  (if (= :element (-> (zip/node hzip-loc) :type))
    hzip-loc))

(def element
  "Another name for the any selector, to express that it can be used to only
   select elements."
  any)

(defn root
  "This selector takes no args, it simply is the selector function. It returns
   the zip-loc of the root node (the HTML element)."
  [hzip-loc]
  (if (= :html (-> (zip/node hzip-loc) :tag))
    hzip-loc))

(defn find-in-text
  "Returns a function that takes a zip-loc argument and returns the zip-loc
   passed in iff it has some text node in its contents that matches the regular
   expression. Note that this only applies to the direct text content of a node;
   nodes which have the given text in one of their child nodes will not be
   selected."
  [re]
  (fn [hzip-loc]
    (some #(re-find re %) (->> (zip/node hzip-loc)
                               :content
                               (filter string?)))))

(defn n-moves-until
  "This selector returns a selector function that selects its argument if
   that argument is some \"distance\" from a \"boundary.\" This is an abstract
   way of phrasing it, but it captures the full generality.

   The selector this function returns will apply the move argument to its own
   output, beginning with its zipper loc argument, until the term-pred argument
   called on its output returns true. At that point, the number of times the
   move function was called successfully is compared to kn+c; if there exists
   some value of k such that the two quantities are equal, then the selector
   will return the argument zipper loc successfully.

   For example, (n-moves-until 2 1 clojure.zip/left nil?) will return a selector
   that calls zip/left on its own output, beginning with the argument zipper
   loc, until its return value is nil (nil? returns true). Suppose it called
   left 5 times before zip/left returned nil. Then the selector will return
   with success, since 2k+1 = 5 for k = 2.

   Most nth-child-* selectors in this package use n-moves-until in their
   implementation."
  [n c move term-pred]
  (fn [hzip-loc]
    (let [distance (count-until move hzip-loc term-pred)]
      (if (== 0 n)
        ;; No stride, so distance must = c to select.
        (if (== distance c)
          hzip-loc)
        ;; There's a stride, so need to subtract c and see if the
        ;; remaining distance is a multiple of n.
        (if (== 0 (rem (- distance c) n))
          hzip-loc)))))

(defn nth-of-type
  "Returns a function that returns true if the node is the nth child of
   its parent (and it has a parent) of the given tag type. First element is 1,
   last is n."
  ([c typ]
     (cond (= :odd c)
           (nth-of-type 2 1 typ)
           (= :even c)
           (nth-of-type 2 0 typ)
           :else
           (nth-of-type 0 c typ)))
  ([n c typ]
     (fn [hzip-loc]
       ;; We're only interested in elements whose parents are also elements,
       ;; so check this up front and maybe save some work.
       (if (clojure.core/and (element hzip-loc)
                     (element (zip/up hzip-loc))
                     (= typ (:tag (zip/node hzip-loc))))
         (let [sel (n-moves-until n c
                                  #(left-pred % (fn [x] (-> (zip/node x)
                                                            :tag
                                                            (= typ))))
                                  nil?)]
           (sel hzip-loc))))))

(defn nth-last-of-type
  "Returns a function that returns true if the node is the nth last child of
   its parent (and it has a parent) of the given tag type. First element is 1,
   last is n."
  ([c typ]
     (cond (= :odd c)
           (nth-last-of-type 2 1 typ)
           (= :even c)
           (nth-last-of-type 2 0 typ)
           :else
           (nth-last-of-type 0 c typ)))
  ([n c typ]
     (fn [hzip-loc]
       ;; We're only interested in elements whose parents are also elements,
       ;; so check this up front and maybe save some work.
       (if (clojure.core/and (element hzip-loc)
                     (element (zip/up hzip-loc))
                     (= typ (:tag (zip/node hzip-loc))))
         (let [sel (n-moves-until n c
                                  #(right-pred % (fn [x] (-> (zip/node x)
                                                             :tag
                                                             (= typ))))
                                  nil?)]
           (sel hzip-loc))))))

(defn nth-child
  "Returns a function that returns true if the node is the nth child of
   its parent (and it has a parent). First element is 1, last is n."
  ([c]
     (cond (= :odd c)
           (nth-child 2 1)
           (= :even c)
           (nth-child 2 0)
           :else
           (nth-child 0 c)))
  ([n c]
     (fn [hzip-loc]
       ;; We're only interested in elements whose parents are also elements,
       ;; so check this up front and maybe save some work.
       (if (clojure.core/and (element hzip-loc)
                     (element (zip/up hzip-loc)))
         (let [sel (n-moves-until n c #(left-of-node-type % :element) nil?)]
           (sel hzip-loc))))))


(defn nth-last-child
  "Returns a function that returns true if the node has n siblings after it,
   and has a parent."
  ([c]
     (cond (= :odd c)
           (nth-last-child 2 1)
           (= :even c)
           (nth-last-child 2 0)
           :else
           (nth-last-child 0 c)))
  ([n c]
     (fn [hzip-loc]
       ;; We're only interested in elements whose parents are also elements,
       ;; so check this up front and maybe save some work.
       (if (clojure.core/and (element hzip-loc)
                     (element (zip/up hzip-loc)))
         (let [sel (n-moves-until n c #(right-of-node-type % :element) nil?)]
           (sel hzip-loc))))))

(defn first-child
  "This selector takes no args, it is simply the selector. Returns
   true if the node is the first child of its parent (and it has a
   parent)."
  [hzip-loc]
  (clojure.core/and (element hzip-loc)
            (element (zip/up hzip-loc))
            ((nth-child 1) hzip-loc)))

(defn last-child
  "This selector takes no args, it is simply the selector. Returns
   true if the node is the last child of its parent (and it has a
   parent."
  [hzip-loc]
  (clojure.core/and (element hzip-loc)
            (element (zip/up hzip-loc))
            ((nth-last-child 1) hzip-loc)))

;;
;; Selector combinators
;;

(defn and
  "Takes any number of selectors and returns a selector that is true if
   all of the argument selectors are true."
  [& selectors]
  (fn [zip-loc]
    (if (every? #(% zip-loc) selectors)
      zip-loc)))

(defn or
  "Takes any number of selectors and returns a selector that is true if
   any of the argument selectors are true."
  [& selectors]
  (fn [zip-loc]
    (if (some #(% zip-loc) selectors)
      zip-loc)))

(defn not
  "Takes a selector argument and returns a selector that is true if
   the underlying selector is false on its argument, and vice versa."
  [selector]
  (fn [hzip-loc]
    (if (clojure.core/not (selector hzip-loc))
      hzip-loc)))

(defn el-not
  "Takes a selector argument and returns a selector that is true if
   the underlying selector is false on its argument and vice versa, and
   additionally that argument is an element node. Compared to the 'not'
   selector, this corresponds more closely to the CSS equivalent, which
   will only ever select elements."
  [selector]
  (and (node-type :element)
       (not selector)))

(defn ordered-adjacent
  "Takes a zipper movement function and any number of selectors as arguments
   and returns a selector that returns true when the zip-loc given as the
   argument is satisfied by the first selector, and the zip-loc arrived at by
   applying the move-fn argument is satisfied by the second selector, and so
   on for all the selectors given as arguments."
  [move-fn & selectors]
  ;; We'll work backwards through the selector list with an index. First we'll
  ;; build the selector list into an array for quicker access. We'll do it
  ;; immediately and then closure-capture the result, so it does not get
  ;; redone every time the selector is called.
  (let [selectors (into-array IFn selectors)]
    (fn [hzip-loc]
      (loop [curr-loc hzip-loc
             idx 0]
        (if (>= idx (count selectors))
          hzip-loc ;; Got to end satisfying selectors, return the loc.
          (if-let [next-loc ((nth selectors idx) curr-loc)]
            (recur (move-fn next-loc)
                   (inc idx))))))))

(defn child
  "Takes any number of selectors as arguments and returns a selector that
   returns true when the zip-loc given as the argument is at the end of
   a chain of direct child relationships specified by the selectors given as
   arguments.

   Example: (child (tag :div) (class :foo) (attr :disabled))
     will select the input in
   <div><span class=\"foo\"><input disabled></input></span></div>
     but not in
   <div><span class=\"foo\"><b><input disabled></input></b></span></div>"
  [& selectors]
  (apply ordered-adjacent zip/up (reverse selectors)))

(defn follow-adjacent
  "Takes any number of selectors as arguments and returns a selector that
   returns true when the zip-loc given as the argument is at the end of
   a chain of direct element sibling relationships specified by the selectors
   given as arguments.

   Example: (follow-adjacent (tag :div) (class :foo))
     will select the span in
   <div>...</div><span class=\"foo\">...</span>
     but not in
   <div>...</div><b>...</b><span class=\"foo\">...</span>"
  [& selectors]
  (apply ordered-adjacent
         #(left-of-node-type % :element)
         (reverse selectors)))

(defn precede-adjacent
  "Takes any number of selectors as arguments and returns a selector that
   returns true when the zip-loc given as the argument is at the beginning of
   a chain of direct element sibling relationships specified by the selectors
   given as arguments.

   Example: (precede-adjacent (tag :div) (class :foo))
     will select the div in
   <div>...</div><span class=\"foo\">...</span>
     but not in
   <div>...</div><b>...</b><span class=\"foo\">...</span>"
  [& selectors]
  (apply ordered-adjacent
         #(right-of-node-type % :element)
         selectors))

(defn ordered
  "Takes a zipper movement function and any number of selectors as arguments
   and returns a selector that returns true when the zip-loc given as the
   argument is satisfied by the first selector, and some zip-loc arrived at by
   applying the move-fn argument *one or more times* is satisfied by the second
   selector, and so on for all the selectors given as arguments."
  [move-fn & selectors]
  ;; This function is a lot like ordered-adjacent, above, but:
  ;; 1) failing to fulfill a selector does not stop us moving along the tree
  ;; 2) therefore, we need to make sure the first selector matches the loc under
  ;;    consideration, and not merely one that is farther along the movement
  ;;    direction.
  (let [selectors (into-array IFn selectors)]
    (fn [hzip-loc]
      ;; First need to check that the first selector matches the current loc,
      ;; or else we can return nil immediately.
      (let [fst-selector (nth selectors 0)]
        (if (fst-selector hzip-loc)
          ;; First selector matches this node, so now check along the
          ;; movement direction for the rest of the selectors.
          (loop [curr-loc (move-fn hzip-loc)
                 idx 1]
            (cond (>= idx (count selectors))
                  hzip-loc ;; Satisfied all selectors, so return the orig. loc.
                  (nil? curr-loc)
                  nil ;; Ran out of movements before selectors, return nil.
                  :else
                  (if ((nth selectors idx) curr-loc)
                    (recur (move-fn curr-loc)
                           (inc idx))
                    ;; Failed, so move but retry the same selector
                    (recur (move-fn curr-loc) idx)))))))))

(defn descendant
  "Takes any number of selectors as arguments and returns a selector that
   returns true when the zip-loc given as the argument is at the end of
   a chain of descendant relationships specified by the
   selectors given as arguments. To be clear, the node selected matches
   the final selector, but the previous selectors can match anywhere in
   the node's ancestry, provided they match in the order they are given
   as arguments, from top to bottom.

   Example: (descendant (tag :div) (class :foo) (attr :disabled))
     will select the input in both
   <div><span class=\"foo\"><input disabled></input></span></div>
     and
   <div><span class=\"foo\"><b><input disabled></input></b></span></div>"
  [& selectors]
  (apply ordered zip/up (reverse selectors)))

(defn follow
  "Takes any number of selectors as arguments and returns a selector that
   returns true when the zip-loc given as the argument is at the end of
   a chain of element sibling relationships specified by the selectors
   given as arguments; intervening elements that do not satisfy a selector
   are simply ignored and do not prevent a match.

   Example: (follow (tag :div) (class :foo))
     will select the span in both
   <div>...</div><span class=\"foo\">...</span>
     and
   <div>...</div><b>...</b><span class=\"foo\">...</span>"
  [& selectors]
  (apply ordered #(left-of-node-type % :element) (reverse selectors)))

(defn precede
  "Takes any number of selectors as arguments and returns a selector that
   returns true when the zip-loc given as the argument is at the beginning of
   a chain of element sibling relationships specified by the selectors
   given as arguments; intervening elements that do not satisfy a selector
   are simply ignored and do not prevent a match.

   Example: (precede (tag :div) (class :foo))
     will select the div in both
   <div>...</div><span class=\"foo\">...</span>
     and
   <div>...</div><b>...</b><span class=\"foo\">...</span>"
  [& selectors]
  (apply ordered #(right-of-node-type % :element) selectors))
