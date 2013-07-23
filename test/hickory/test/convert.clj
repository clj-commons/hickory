(ns hickory.test.convert
  (:use clojure.test
        hickory.convert
        hickory.core))

(deftest hiccup-to-hickory-test
  (is (= (as-hickory (parse "<i>Hi.</i>"))
         (hiccup-to-hickory (as-hiccup (parse "<i>Hi.</i>")))))
  (is (= (as-hickory (parse "<i>Outer<b>Inner.</b></i>"))
         (hiccup-to-hickory (as-hiccup (parse "<i>Outer<b>Inner.</b></i>")))))
  (is (= (as-hickory (parse "<a href='http://localhost/?a=1&amp;b=2'>Hi</a>"))
         (hiccup-to-hickory (as-hiccup (parse "<a href='http://localhost/?a=1&amp;b=2'>Hi</a>")))))
  (is (= (as-hickory (parse "<script>alert();</script>"))
         (hiccup-to-hickory (as-hiccup (parse "<script>alert();</script>"))))))

