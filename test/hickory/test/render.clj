(ns hickory.test.render
  (:use clojure.test
        hickory.core
        hickory.render))

;;
;; Hickory to HTML
;;

(deftest hickory-to-html-test
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

(deftest hickory-doctypes-test
  (is (= "<!DOCTYPE html><html><head></head><body></body></html>"
         (hickory-to-html (as-hickory (parse "<!DOCTYPE html><html><head></head><body></body></html>")))))
  (is (= "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\"><html><head></head><body></body></html>"
         (hickory-to-html (as-hickory (parse "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\"><html><head></head><body></body</html>"))))))

(deftest error-handling-test
  (let [data {:type :foo :tag :a :attrs {:foo "bar"}}]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"^Not a valid node: nil"
          (hickory-to-html nil)))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"^Not a valid node: \{:type :foo, :attrs \{:foo \"bar\"\}, :tag :a\}"
          (hickory-to-html data)))
    (is (= data 
           (try (hickory-to-html data)
                (catch Exception e (:dom (ex-data e))))))))

;;
;; Hiccup to HTML
;;

(deftest hiccup-to-html-test
  (is (= "<!DOCTYPE html><html><head></head><body><p><!--hi--><a href=\"foo\" id=\"bar\">hi</a></p></body></html>"
         (hiccup-to-html (as-hiccup (parse "<!DOCTYPE html><P><!--hi--><a href=foo id=\"bar\">hi")))))
  ;; Make sure void elements don't have closing tags.
  (is (= "<html><head></head><body>Hi<br>There</body></html>"
         (hiccup-to-html (as-hiccup (parse "<html><head></head><body>Hi<br>There</body></html>")))))
  ;; Make sure text is properly escaped.
  (is (= "<code>&lt;html&gt;</code>"
         (hiccup-to-html [(as-hiccup (first (parse-fragment "<code>&lt;html&gt;</code>")))])))
  ;; Make sure the contents of script/style tags do not get html escaped.
  (is (= "<script>Test<!--Test&Test-->Test</script>"
         (hiccup-to-html [(as-hiccup
                           (first (parse-fragment "<script>Test<!--Test&Test-->Test</script>")))])))
  ;; Make sure attribute contents are html-escaped.
  (is (= "<img fake-attr=\"abc&quot;def\">"
         (hiccup-to-html [(as-hiccup (first (parse-fragment "<img fake-attr=\"abc&quot;def\">")))]))))

(deftest hiccup-doctypes-test
  (is (= "<!DOCTYPE html><html><head></head><body></body></html>"
         (hiccup-to-html (as-hiccup (parse "<!DOCTYPE html><html><head></head><body></body></html>")))))
  (is (= "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\"><html><head></head><body></body></html>"
         (hiccup-to-html (as-hiccup (parse "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\"><html><head></head><body></body</html>"))))))
