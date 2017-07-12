(ns hickory.test.core
  (:require [hickory.core :refer [as-hickory as-hiccup parse parse-fragment]]
    #?(:clj
            [clojure.test :refer :all]
       :cljs [cljs.test :refer-macros [is are deftest testing use-fixtures]])))

;; This document tests: doctypes, white space text nodes, attributes,
;; and cdata nodes.
(deftest basic-documents
  (is (= ["<!DOCTYPE html>"
          [:html {}
           [:head {}]
           [:body {}
            [:a {:href "foo"} "foo"] " "
            [:a {:id "so", :href "bar"} "bar"]
            [:script {:src "blah.js"} "alert(\"hi\");"]]]]
         (as-hiccup (parse "<!DOCTYPE html><a href=\"foo\">foo</a> <a id=\"so\" href=\"bar\">bar</a><script src=\"blah.js\">alert(\"hi\");</script>"))))
  (is (= {:type :document,
          :content [{:type :document-type,
                     :attrs {:name "html", :publicid "", :systemid ""}}
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
         (as-hickory (parse "<!DOCTYPE html><a href=\"foo\">foo</a> <a id=\"so\" href=\"bar\">bar</a><script src=\"blah.js\">alert(\"hi\");</script>")))))

;; This document tests: doctypes, comments, white space text nodes, attributes,
;; and cdata nodes.
(deftest basic-documents2
  (is (= ["<!DOCTYPE html>"
          [:html {}
           [:head {}]
           [:body {}
            "<!--comment-->"
            [:a {:href "foo"} "foo"] " "
            [:a {:id "so", :href "bar"} "bar"]
            [:script {:src "blah.js"} "alert(\"hi\");"]]]]
         (as-hiccup (parse "<!DOCTYPE html><body><!--comment--><a href=\"foo\">foo</a> <a id=\"so\" href=\"bar\">bar</a><script src=\"blah.js\">alert(\"hi\");</script></body>"))))
  (is (= {:type :document,
          :content [{:type :document-type,
                     :attrs {:name "html", :publicid "", :systemid ""}}
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
                                :content [{:type :comment
                                           :content ["comment"]}
                                          {:type :element,
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
         (as-hickory (parse "<!DOCTYPE html><body><!--comment--><a href=\"foo\">foo</a> <a id=\"so\" href=\"bar\">bar</a><script src=\"blah.js\">alert(\"hi\");</script></body>")))))

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

(deftest unencoded-text-nodes
  ;; Hiccup versions - Note that hiccup representation does not html-escape any
  ;; strings that aren't attribute values, so the hiccup representation will
  ;; have the string contents html-escaped.
  (is (= [[:html {} [:head {}] [:body {} [:p {} "ABC&amp;\n\nDEF."]]]]
         (as-hiccup (parse "<p>ABC&amp;\n\nDEF.</p>"))))
  ;; <pre> tag preserves whitespace.
  (is (= [[:html {} [:head {}] [:body {} [:pre {} "ABC&amp;\n\nDEF."]]]]
         (as-hiccup (parse "<pre>ABC&amp;\n\nDEF.</pre>"))))
  ;; Hickory versions - Note that the representation is different, and Hickory
  ;; format does not keep HTML escaped in its representation, as it can
  ;; figure out what to escape at render time.
  (is (= "ABC&\n\nDEF."
         (get-in (as-hickory (parse "<p>ABC&amp;\n\nDEF.</p>"))
                 [:content 0 :content 1 :content 0 :content 0])))
  ;; <pre> tag preserves whitespace.
  (is (= "ABC&\n\nDEF."
         (get-in (as-hickory (parse "<pre>ABC&amp;\n\nDEF.</pre>"))
                 [:content 0 :content 1 :content 0 :content 0]))))

;; Issue #50: Tests that the parser does not throw a StackOverflowError when
;; parsing a document with deeply nested HTML tags.
(deftest deeply-nested-tags
  (let [jsoup (parse (apply str (repeat 2048 "<font>abc")))]
    (is (= [:font {} "abc"]
           (get-in (vec (as-hiccup jsoup))
                   (concat [0 3 2] (repeat 2047 3)))))
    (is (= {:type :element
            :attrs nil
            :tag :font
            :content ["abc"]}
           (get-in (as-hickory jsoup)
                   (apply concat
                          [:content 0 :content 1 :content 0]
                          (repeat 2047 [:content 1])))))))
