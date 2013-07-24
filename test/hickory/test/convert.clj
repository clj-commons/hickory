(ns hickory.test.convert
  (:use clojure.test
        hickory.convert
        hickory.core))

(deftest hiccup-to-hickory-test
  (is (= (as-hickory (parse "<i>Hi.</i>"))
         (hiccup-to-hickory (as-hiccup (parse "<i>Hi.</i>")))))
  (is (= (as-hickory (parse "<i>Outer<b class=\"foo\">Inner.</b></i>"))
         (hiccup-to-hickory (as-hiccup (parse "<i>Outer<b class=\"foo\">Inner.</b></i>")))))
  (is (= (as-hickory (parse "<a href='http://localhost/?a=1&amp;b=2'>Hi</a>"))
         (hiccup-to-hickory (as-hiccup (parse "<a href='http://localhost/?a=1&amp;b=2'>Hi</a>")))))
  (is (= (as-hickory (parse "<script>alert();</script>"))
         (hiccup-to-hickory (as-hiccup (parse "<script>alert();</script>"))))))

(deftest hiccup-fragment-to-hickory-test
  (is (= (map as-hickory (parse-fragment "<img src=\"a.jpg\">"))
         (hiccup-fragment-to-hickory (map as-hiccup (parse-fragment "<img src=\"a.jpg\">")))))
  (let [src "<a href=\"/a.txt\"><img src=\"a.jpg\"></a><b>It's an a.</b>"]
    (is (= (map as-hickory (parse-fragment src))
           (hiccup-fragment-to-hickory (map as-hiccup (parse-fragment src)))))))


(deftest hickory-to-hiccup-test
  (is (= (as-hiccup (parse "<i>Hi.</i>"))
         (hickory-to-hiccup (as-hickory (parse "<i>Hi.</i>")))))
  (is (= (as-hiccup (parse "<i>Outer<b class=\"foo\">Inner.</b></i>"))
         (hickory-to-hiccup (as-hickory (parse "<i>Outer<b class=\"foo\">Inner.</b></i>")))))
  (is (= (as-hiccup (parse "<a href='http://localhost/?a=1&amp;b=2'>Hi</a>"))
         (hickory-to-hiccup (as-hickory (parse "<a href='http://localhost/?a=1&amp;b=2'>Hi</a>")))))
  (is (= (as-hiccup (parse "<script>alert();</script>"))
         (hickory-to-hiccup (as-hickory (parse "<script>alert();</script>")))))
  ;; Fragments
  (is (= (map as-hiccup (parse-fragment "<img src=\"a.jpg\">"))
         (map hickory-to-hiccup (map as-hickory (parse-fragment "<img src=\"a.jpg\">")))))
  (let [src "<a href=\"/a.txt\"><img src=\"a.jpg\"></a><b>It's an a.</b>"]
    (is (= (map as-hiccup (parse-fragment src))
           (map hickory-to-hiccup (map as-hickory (parse-fragment src)))))))

