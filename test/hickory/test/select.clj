(ns hickory.test.select
  (:use clojure.test)
  (:require [hickory.core :as hickory]
            [hickory.select :as select]
            [clojure.zip :as zip]))

(def html1
  "<!DOCTYPE html>
<!-- Comment 1 -->
<html>
<body>
<h1>Heading</h1>
<p>Paragraph</p>
<a href=\"http://example.com\">Link</a>
<div class=\"aclass bclass cool\">
<div class=\"subdiv cool\" id=\"deepestdiv\">Div</div>
<!-- Comment 2 -->
<span id=\"anid\" class=\"cool\">Span</span>
</div>
</body>
</html>")

(deftest select-test
  (testing "The select function."
    (let [htree (hickory/as-hickory (hickory/parse html1))]
      (let [selection (select/select (fn [zip-loc]
                                       (= (:type (zip/node zip-loc))
                                          :document-type))
                                     htree)]
        (is (and (= 1 (count selection))
                 (= :document-type
                    (-> selection first :type)))))
      
      )))

(deftest has-class?-test
  (testing "has-class? function"
    (let [htree (hickory/as-hickory (hickory/parse html1))]
      (let [selection (select/select (select/has-class? "aclass")
                                     htree)]
        (is (and (= 1 (count selection))
                 (re-find #"aclass"
                          (-> selection first :attrs :class)))))
      (let [selection (select/select (select/has-class? "cool")
                                     htree)]
        (is (and (= 3 (count selection))
                 (every? #(not (nil? %))
                         (map #(re-find #"cool"
                                        (-> % :attrs :class))
                              selection))))))))

