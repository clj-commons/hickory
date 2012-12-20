(ns hickory.test.core
  (:use clojure.test
        hickory.core))

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
         (as-hickory (parse "<!DOCTYPE html><!--comment--><a href=\"foo\">foo</a> <a id=\"so\" href=\"bar\">bar</a><script src=\"blah.js\">alert(\"hi\");</script>")))))

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
  (is (= [[:html {} [:head {}] [:body {} [:p {} "ABC&\n\nDEF."]]]]
         (as-hiccup (parse "<p>ABC&amp;\n\nDEF.</p>"))))
  ;; <pre> tag preserves whitespace.
  (is (= [[:html {} [:head {}] [:body {} [:pre {} "ABC&\n\nDEF."]]]]
         (as-hiccup (parse "<pre>ABC&amp;\n\nDEF.</pre>"))))
  ;; Hickory versions
  (is (= "ABC&\n\nDEF."
         (get-in (as-hickory (parse "<p>ABC&amp;\n\nDEF.</p>"))
                 [:content 0 :content 1 :content 0 :content 0])))
  ;; <pre> tag preserves whitespace.
  (is (= "ABC&\n\nDEF."
         (get-in (as-hickory (parse "<pre>ABC&amp;\n\nDEF.</pre>"))
                 [:content 0 :content 1 :content 0 :content 0]))))

(deftest html-output
  (is (= "<!DOCTYPE html><html><head></head><body><p><!--hi--><a href=\"foo\" id=\"bar\">hi</a></p></body></html>"
         (hickory-to-html (as-hickory (parse "<!DOCTYPE html><P><!--hi--><a href=foo id=\"bar\">hi")))))
  ;; Make sure void elements don't have closing tags.
  (is (= "<html><head></head><body>Hi<br>There</body></html>"
         (hickory-to-html (as-hickory (parse "<html><head></head><body>Hi<br>There</body></html>")))))
  ;; Make sure text is properly escaped.
  (is (= "<code>&lt;html&gt;</code>"
         (hickory-to-html (as-hickory (first (parse-fragment "<code>&lt;html&gt;</code>"))))))
  ;; Make sure the contents of script/style tags do not get html escaped.
  (is (= "<script>Test<!--Test&Test-->Test</script>"
         (hickory-to-html (as-hickory
                           (first (parse-fragment "<script>Test<!--Test&Test-->Test</script>"))))))
  ;; Make sure attribute contents are html-escaped.
  (is (= "<img fake-attr=\"abc&quot;def\">"
         (hickory-to-html (as-hickory (first (parse-fragment "<img fake-attr=\"abc&quot;def\">")))))))

(deftest doctypes
  (is (= "<!DOCTYPE html><html><head></head><body></body></html>"
         (hickory-to-html (as-hickory (parse "<!DOCTYPE html><html><head></head><body></body></html>")))))
  (is (= "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\"><html><head></head><body></body></html>"
         (hickory-to-html (as-hickory (parse "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\"><html><head></head><body></body</html>"))))))