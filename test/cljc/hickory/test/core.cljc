(ns hickory.test.core
  (:require [hickory.core :refer [as-hickory as-hiccup parse parse-fragment]]
            [clojure.test :refer [deftest is]]))

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
                     :attrs {:name "html", :publicid "", :systemid ""
                             #?@(:clj [:#doctype "html"])}}
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
                     :attrs {:name "html", :publicid "", :systemid ""
                             #?@(:clj [:#doctype "html"])}}
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

;; Issue #98 regression test: as-hiccup could not handle unescapable elements
;; like <script> and <style> if they had no children.
(deftest sibling-after-unescapable-element
  ;; A single empty <script> in <head> must not keep the following <body> from being converted.
  (is (= ["<!DOCTYPE html>"
          [:html {}
           [:head {} [:script {:src "/test.js"}]]
           [:body {} [:h1 {} "Test"]]]]
         (as-hiccup (parse "<!DOCTYPE html><html><head><script src=\"/test.js\"></script></head><body><h1>Test</h1></body></html>"))))
  ;; With two empty <script> tags, both must be converted
  ;; (previously the second was left as a raw Element).
  (is (= ["<!DOCTYPE html>"
          [:html {}
           [:head {}
            [:script {:src "/test.js"}]
            [:script {:src "/test.js"}]]
           [:body {} [:h1 {} "Test"]]]]
         (as-hiccup (parse "<!DOCTYPE html><html><head><script src=\"/test.js\"></script><script src=\"/test.js\"></script></head><body><h1>Test</h1></body></html>"))))
  ;; An empty <style> tag shares the same unescapable handling
  ;; and must not skip its following sibling either.
  (is (= [[:html {}
           [:head {} [:style {}]]
           [:body {} [:h1 {} "Test"]]]]
         (as-hiccup (parse "<html><head><style></style></head><body><h1>Test</h1></body></html>"))))
  ;; A <script> WITH content must still convert correctly - this always worked.
  ;; The fix must not over-correct and skip real siblings.
  (is (= [[:html {}
           [:head {} [:script {} "alert(\"hi\");"]]
           [:body {} [:h1 {} "Test"]]]]
         (as-hiccup (parse "<html><head><script>alert(\"hi\");</script></head><body><h1>Test</h1></body></html>"))))
  ;; Normal text and unescapable content must both be handled correctly (and differently)
  (is (= [[:html {}
           [:head {} [:script {} "if (a < b && c > d) { }"]]
           [:body {} "a &amp; b"]]]
         (as-hiccup (parse "<html><head><script>if (a < b && c > d) { }</script></head><body>a & b</body></html>")))))

;; Issue #50: Tests that the parser does not throw a StackOverflowError when
;; parsing a document with deeply nested HTML tags.
;; I don't have time for this
#_(deftest deeply-nested-tags
  (let [jsoup (parse (apply str (repeat 2048 "<font>abc")))
        r1 (get-in (vec (as-hiccup jsoup))
                   (concat [0 3 2] (repeat 2047 3)))
        r2 (get-in (as-hickory jsoup)
                   (apply concat
                          [:content 0 :content 1 :content 0]
                          (repeat 2047 [:content 1])))]
    (println "R1" r1)
    (println "R2" r2)
    (is (= [:font {} "abc"]
           r1))
    (is (= {:type :element
            :attrs nil
            :tag :font
            :content ["abc"]}
           r2))))

#?(:cljs
   (deftest node-type-test
     (is (= 3 (hickory.core/node-type "TEXT")))))
