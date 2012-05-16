(ns hickory.test.core
  (:use clojure.test
        hickory.core))

(deftest basic-documents
  (is (= ["<!DOCTYPE html>"
          [:html {}
           [:head {}]
           [:body {}
            [:a {:href "foo"} "foo"] " "
            [:a {:id "so", :href "bar"} "bar"]
            [:script {:src "blah.js"} "alert(\"hi\");"]]]]
         (parse "<!DOCTYPE html><a href=\"foo\">foo</a> <a id=\"so\" href=\"bar\">bar</a><script src=\"blah.js\">alert(\"hi\");</script>"))))