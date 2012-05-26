(ns hickory.test.core
  (:use clojure.test
        hickory.core)
  (:require [clojure.zip :as zip]))

;; This document tests: doctypes, comments, white space text nodes, attributes,
;; and cdata nodes.
(deftest basic-documents
  (is (= ["<!DOCTYPE html>"
          "<!--comment-->"
          [:html {}
           [:head {}]
           [:body {}
            [:a {:href "foo"} "foo"] " "
            [:a {:id "so", :href "bar"} "bar"]
            [:script {:src "blah.js"} "alert(\"hi\");"]]]]
         (as-hiccup (parse "<!DOCTYPE html><!--comment--><a href=\"foo\">foo</a> <a id=\"so\" href=\"bar\">bar</a><script src=\"blah.js\">alert(\"hi\");</script>"))))
  (is (= {:type :document,
          :content [{:type :document-type,
                     :attrs {:name "html", :publicid "", :systemid ""}}
                    {:type :comment
                     :content ["comment"]}
                    {:type :element,
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
                                           :attrs {:href "foo"},
                                           :tag :a,
                                           :content ["foo"]}
                                          " "
                                          {:type :element,
                                           :attrs {:id "so", :href "bar"},
                                           :tag :a,
                                           :content ["bar"]}
                                          {:type :element,
                                           :attrs {:src "blah.js"},
                                           :tag :script,
                                           :content ["alert(\"hi\");"]}]}]}]}
         (as-hickory (parse "<!DOCTYPE html><!--comment--><a href=\"foo\">foo</a> <a id=\"so\" href=\"bar\">bar</a><script src=\"blah.js\">alert(\"hi\");</script>"))))
  )

;; Want to test a document fragment that has multiple nodes with no parent,
;; as well as a text node between nodes.
(deftest basic-document-fragment
  (is (= [[:a {:href "foo"} "foo"] " "
          [:a {:href "bar"} "bar"]]
         (map as-hiccup
              (parse-fragment "<a href=\"foo\">foo</a> <a href=\"bar\">bar</a>"))))
  (is (= [{:type :element,
           :attrs {:href "foo"},
           :tag :a,
           :content ["foo"]}
          " "
          {:type :element,
           :attrs {:href "bar"},
           :tag :a,
           :content ["bar"]}]
         (map as-hickory
              (parse-fragment "<a href=\"foo\">foo</a> <a href=\"bar\">bar</a>")))))

(deftest html-zipper
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

(deftest html-output
  (is (= "<!DOCTYPE html><html><head></head><body><p><!--hi--><a href=\"foo\" id=\"bar\">hi</a></p></body></html>"
         (dom-to-html (as-hickory (parse "<!DOCTYPE html><P><!--hi--><a href=foo id=\"bar\">hi")))))
  ;; Make sure void elements don't have closing tags.
  (is (= "<html><head></head><body>Hi<br>There</body></html>"
         (dom-to-html (as-hickory (parse "<html><head></head><body>Hi<br>There</body></html>"))))))