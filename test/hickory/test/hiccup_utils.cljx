(ns hickory.test.hiccup-utils
  #+clj (:use clojure.test)
  (:require [hickory.hiccup-utils :refer [class-names id normalize-form tag-name tag-well-formed?]]
            #+cljs [cemerick.cljs.test :as t])
  #+cljs (:require-macros [cemerick.cljs.test :refer (is deftest)]))

#+clj
(deftest first-idx-test
  (let [first-idx #'hickory.hiccup-utils/first-idx]
    (is (= -1 (first-idx -1 -1)))
    (is (= 2 (first-idx -1 2)))
    (is (= 5 (first-idx 5 -1)))
    (is (= 3 (first-idx 5 3)))
    (is (= 3 (first-idx 3 5)))))

(deftest tag-well-formed?-test
  (is (= true (tag-well-formed? :a)))
  (is (= true (tag-well-formed? :a#id)))
  (is (= true (tag-well-formed? :a#id.class)))
  (is (= true (tag-well-formed? :a.class.class2)))
  (is (= false (tag-well-formed? "")))
  (is (= false (tag-well-formed? ".class")))
  (is (= false (tag-well-formed? "#id.class")))
  (is (= false (tag-well-formed? :a.class#id)))
  (is (= false (tag-well-formed? :a#id#id2))))

(deftest tag-name-test
  (is (= "a" (tag-name "a")))
  (is (= "a" (tag-name 'a)))
  (is (= "a" (tag-name :a)))
  (is (= "b" (tag-name :b.class)))
  (is (= "b" (tag-name :b#id)))
  (is (= "b" (tag-name :b.class#id)))
  (is (= "b" (tag-name :b#id.class))))

(deftest class-names-test
  (is (= [] (class-names :a)))
  (is (= [] (class-names :a#foo)))
  (is (= ["foo"] (class-names "a.foo")))
  (is (= ["bar"] (class-names :a#foo.bar)))
  (is (= ["foo" "bar"] (class-names :a.foo.bar))))

(deftest id-test
  (is (= nil (id :a)))
  (is (= nil (id 'a)))
  (is (= "foo" (id :a#foo)))
  (is (= "foo" (id :a#foo.bar))))

#+clj
(deftest expand-content-seqs-test
  (let [expand-content-seqs #'hickory.hiccup-utils/expand-content-seqs]
    (is (= [1 2 3] (expand-content-seqs [1 2 3])))
    (is (= [1 2 [3]] (expand-content-seqs [1 '(2 [3])])))
  ;; Example from docstring.
    (is (= [1 2 3 2 4 6 [5]]
           (expand-content-seqs [1 '(2 3) (for [x [1 2 3]] (* x 2)) [5]])))))

#+clj
(deftest normalize-element-test
  (let [normalize-element #'hickory.hiccup-utils/normalize-element]
    (is (= [:a {:id nil :class nil} "Hi"] (normalize-element [:a "Hi"])))
    (is (= [:a {:id "foo" :class nil} "Hi"]
           (normalize-element [:A#foo "Hi"])))
    (is (= [:a {:id nil :class "foo"} "Hi"]
           (normalize-element [:a.foo "Hi"])))
    (is (= [:a {:id "foo" :class "bar"} "Hi" "There"]
           (normalize-element [:a#foo.bar "Hi" "There"])))
    (is (= [:a {:id "foo" :class "bar"} "Hi"]
           (normalize-element [:a.bar {:id "foo"} "Hi"])))
    (is (= [:a {:id "foo" :class "bar"}]
           (normalize-element [:A#bip {:id "foo" :class "bar"}])))
    (is (= [:a {:id "foo" :class "bar"}]
           (normalize-element [:a#bip.baz {:id "foo" :class "bar"}])))
    (is (= [:a {:id nil :class "foo bar"}]
           (normalize-element [:a.foo.bar])))))

(deftest normalize-form-test
  (is (= [:a {:id nil :class nil}] (normalize-form [:A])))
  (is (= [:a {:id nil :class nil :href "localhost"}]
         (normalize-form [:a {:href "localhost"}])))
  (is (= [:a {:id nil :class nil}
          [:b {:id nil :class nil} "foo"]
          [:i {:id nil :class nil} "bar"]]
         (normalize-form [:a [:b "foo"] [:i "bar"]])))
  (is (= [:a {:id nil :class nil}
          [:b {:id nil :class nil} "foo"]
          [:i {:id nil :class nil} "bar"]]
         (normalize-form [:a '([:b "foo"] [:i "bar"])])))
  (is (= [:a {:id nil :class nil}
          [:b {:id nil :class nil} "foo" [:i {:id nil :class nil} "bar"]]]
         (normalize-form [:a [:b "foo" [:i "bar"]]]))))

