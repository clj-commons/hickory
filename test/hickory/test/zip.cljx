(ns hickory.test.zip
  #+clj (:use clojure.test)
  (:require [clojure.zip :as zip]
            [hickory.core :refer [as-hiccup as-hickory parse]]
            [hickory.zip :refer [hickory-zip hiccup-zip]]
            #+cljs [cemerick.cljs.test :as t])
  #+cljs (:require-macros [cemerick.cljs.test :refer (is deftest tthrown-with-msg?)]))

(deftest hickory-zipper
  (is (= {:type :document,
          :content [{:type :element,
                     :attrs nil,
                     :tag :html,
                     :content [{:type :element,
                                :attrs nil,
                                :tag :head,
                                :content nil}
                               {:type :element,
                                :attrs nil,
                                :tag :body,
                                :content [{:type :element,
                                           :attrs nil,
                                           :tag :a,
                                           :content nil}]}]}]}
         (zip/node (hickory-zip (as-hickory (parse "<a>"))))))
  (is (= {:type :element,
          :attrs nil,
          :tag :html,
          :content [{:type :element,
                     :attrs nil,
                     :tag :head,
                     :content nil}
                    {:type :element,
                     :attrs nil,
                     :tag :body,
                     :content [{:type :element,
                                :attrs nil,
                                :tag :a,
                                :content nil}]}]}
         (-> (hickory-zip (as-hickory (parse "<a>")))
             zip/next zip/node)))
  (is (= {:type :element, :attrs nil, :tag :head, :content nil}
       (-> (hickory-zip (as-hickory (parse "<a>")))
           zip/next zip/next zip/node)))
  (is (= {:type :element,
          :attrs nil,
          :tag :body,
          :content [{:type :element,
                     :attrs nil,
                     :tag :a,
                     :content nil}]}
         (-> (hickory-zip (as-hickory (parse "<a>")))
             zip/next zip/next zip/next zip/node)))
  (is (= {:type :element,
          :attrs nil,
          :tag :html,
          :content [{:type :element,
                     :attrs nil,
                     :tag :head,
                     :content nil}
                    {:type :element,
                     :attrs nil,
                     :tag :body,
                     :content [{:type :element,
                                :attrs nil,
                                :tag :a,
                                :content nil}]}]}
         (-> (hickory-zip (as-hickory (parse "<a>")))
             zip/next zip/next zip/next zip/up zip/node))))

(deftest hiccup-zipper
  (is (= '([:html {} [:head {}] [:body {} [:a {}]]])
         (zip/node (hiccup-zip (as-hiccup (parse "<a>"))))))
  (is (= [:html {} [:head {}] [:body {} [:a {}]]]
         (-> (hiccup-zip (as-hiccup (parse "<a>")))
             zip/next zip/node)))
  (is (= [:head {}]
         (-> (hiccup-zip (as-hiccup (parse "<a>")))
             zip/next zip/next zip/node)))
  (is (= [:body {} [:a {}]]
         (-> (hiccup-zip (as-hiccup (parse "<a>")))
             zip/next zip/next zip/next zip/node)))
  (is (= [:html {} [:head {}] [:body {} [:a {}]]]
         (-> (hiccup-zip (as-hiccup (parse "<a>")))
             zip/next zip/next zip/next zip/up zip/node))))
