(ns hickory.hiccup-utils
  "Utilities for working with hiccup forms."
  (:require [clojure.string :as str]))

(defn- first-idx
  "Given two possible indexes, returns the lesser that is not -1. If both
   are -1, then -1 is returned. Useful for searching strings for multiple
   markers, as many routines will return -1 for not found.

   Examples: (first-idx -1 -1) => -1
             (first-idx -1 2) => 2
             (first-idx 5 -1) => 5
             (first-idx 5 3) => 3"
  #?(:clj  [^long a ^long b]
     :cljs [a b])
  (if (== a -1)
    b
    (if (== b -1)
      a
      (min a b))))

(defn- index-of
  ([^String s c]
    #?(:clj  (.indexOf s (int c))
       :cljs (.indexOf s c)))
  ([^String s c idx]
    #?(:clj  (.indexOf s (int c) (int idx))
       :cljs (.indexOf s c idx))))

(defn- split-keep-trailing-empty
  "clojure.string/split is a wrapper on java.lang.String/split with the limit
   parameter equal to 0, which keeps leading empty strings, but discards
   trailing empty strings. This makes no sense, so we have to write our own
   to keep the trailing empty strings."
  [s re]
  (str/split s re -1))

(defn tag-well-formed?
  "Given a hiccup tag element, returns true iff the tag is in 'valid' hiccup
   format. Which in this function means:
      1. Tag name is non-empty.
      2. If there is an id, there is only one.
      3. If there is an id, it is nonempty.
      4. If there is an id, it comes before any classes.
      5. Any class name is nonempty."
  [tag-elem]
  (let [tag-elem (name tag-elem)
        hash-idx (int (index-of tag-elem \#))
        dot-idx (int (index-of tag-elem \.))
        tag-cutoff (first-idx hash-idx dot-idx)]
    (and (< 0 (count tag-elem)) ;; 1.
         (if (== tag-cutoff -1) true (> tag-cutoff 0)) ;; 1.
         (if (== hash-idx -1)
           true
           (and (== -1 (index-of tag-elem \# (inc hash-idx))) ;; 2.
                (< (inc hash-idx) (first-idx (index-of tag-elem \. ;; 3.
                                                       (inc hash-idx))
                                             (count tag-elem)))))
         (if (and (not= hash-idx -1) (not= dot-idx -1)) ;; 4.
           (< hash-idx dot-idx)
           true)
         (if (== dot-idx -1) ;; 5.
           true
           (let [classes (.substring tag-elem (inc dot-idx))]
             (every? #(< 0 (count %))
                     (split-keep-trailing-empty classes #"\.")))))))

(defn tag-name
  "Given a well-formed hiccup tag element, return just the tag name as
  a string."
  [tag-elem]
  (let [tag-elem (name tag-elem)
        hash-idx (int (index-of tag-elem \#))
        dot-idx (int (index-of tag-elem \.))
        cutoff (first-idx hash-idx dot-idx)]
    (if (== cutoff -1)
      ;; No classes or ids, so the entire tag-element is the name.
      tag-elem
      ;; There was a class or id, so the tag name ends at the first
      ;; of those.
      (.substring tag-elem 0 cutoff))))

(defn class-names
  "Given a well-formed hiccup tag element, return a vector containing
   any class names included in the tag, as strings. Ignores the hiccup
   requirement that any id on the tag must come
   first. Example: :div.foo.bar => [\"foo\" \"bar\"]."
  [tag-elem]
  (let [tag-elem (name tag-elem)]
    (loop [curr-dot (index-of tag-elem \.)
           classes (transient [])]
      (if (== curr-dot -1)
        ;; Didn't find another dot, so no more classes.
        (persistent! classes)
        ;; There's another dot, so there's another class.
        (let [next-dot (index-of tag-elem \. (inc curr-dot))
              next-hash (index-of tag-elem \# (inc curr-dot))
              cutoff (first-idx next-dot next-hash)]
          (if (== cutoff -1)
            ;; Rest of the tag element is the last class.
            (recur next-dot
                   (conj! classes (.substring tag-elem (inc curr-dot))))
            ;; The current class name is terminated by another element.
            (recur next-dot
                   (conj! classes
                          (.substring tag-elem (inc curr-dot) cutoff)))))))))

(defn id
  "Given a well-formed hiccup tag element, return a string containing
   the id, or nil if there isn't one."
  [tag-elem]
  (let [tag-elem (name tag-elem)
        hash-idx (int (index-of tag-elem \#))
        next-dot-idx (int (index-of tag-elem \. hash-idx))]
    (if (== hash-idx -1)
      nil
      (if (== next-dot-idx -1)
        (.substring tag-elem (inc hash-idx))
        (.substring tag-elem (inc hash-idx) next-dot-idx)))))

(defn- expand-content-seqs
  "Given a sequence of hiccup forms, presumably the content forms of another
   hiccup element, return a new sequence with any sequence elements expanded
   into the main sequence. This logic does not apply recursively, so sequences
   inside sequences won't be expanded out. Also note that this really only
   applies to sequences; things that seq? returns true on. So this excludes
   vectors.
     (expand-content-seqs [1 '(2 3) (for [x [1 2 3]] (* x 2)) [5]])
     ==> (1 2 3 2 4 6 [5])"
  [content]
  (loop [remaining-content content
         result (transient [])]
    (if (nil? remaining-content)
      (persistent! result)
      (if (seq? (first remaining-content))
        (recur (next remaining-content)
               ;; Fairly unhappy with this nested loop, but it seems
               ;; necessary to continue the handling of transient vector.
               (loop [remaining-seq (first remaining-content)
                      result result]
                 (if (nil? remaining-seq)
                   result
                   (recur (next remaining-seq)
                          (conj! result (first remaining-seq))))))
        (recur (next remaining-content)
               (conj! result (first remaining-content)))))))

(defn- normalize-element
  "Given a well-formed hiccup form, ensure that it is in the form
     [tag attributes content1 ... contentN].
   That is, an unadorned tag name (keyword, lowercase), all attributes in the
   attribute map in the second element, and then any children. Note that this
   does not happen recursively; content is not modified."
  [hiccup-form]
  (let [[tag-elem & content] hiccup-form]
    (when (not (tag-well-formed? tag-elem))
      (throw (ex-info (str "Invalid input: Tag element"
                           tag-elem "is not well-formed.")
                      {})))
    (let [tag-name (keyword (str/lower-case (tag-name tag-elem)))
          tag-classes (class-names tag-elem)
          tag-id (id tag-elem)
          tag-attrs {:id tag-id
                     :class (if (not (empty? tag-classes))
                              (str/join " " tag-classes))}
          [map-attrs content] (if (map? (first content))
                                [(first content) (rest content)]
                                [nil content])
          ;; Note that we replace tag attributes with map attributes, without
          ;; merging them. This is to match hiccup's behavior.
          attrs (merge tag-attrs map-attrs)]
      (apply vector tag-name attrs content))))

(defn normalize-form
  "Given a well-formed hiccup form, recursively normalizes it, so that it and
   all children elements will also be normalized. A normalized form is in the
   form
     [tag attributes content1 ... contentN].
   That is, an unadorned tag name (keyword, lowercase), all attributes in the
   attribute map in the second element, and then any children. Any content
   that is a sequence is also expanded out into the main sequence of content
   items."
  [form]
  (if (string? form)
    form
    ;; Do a pre-order walk and save the first two items, then do the children,
    ;; then glue them back together.
    (let [[tag attrs & contents] (normalize-element form)]
      (apply vector tag attrs (map #(if (vector? %)
                                      ;; Recurse only on vec children.
                                      (normalize-form %)
                                      %)
                                   (expand-content-seqs contents))))))
