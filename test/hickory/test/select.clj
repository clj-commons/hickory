(ns hickory.test.select
  (:use clojure.test)
  (:require [hickory.core :as hickory]
            [hickory.select :as select]
            [hickory.zip :as hzip]
            [clojure.zip :as zip]))

(def html1
  "<!DOCTYPE html>
<!-- Comment 1 -->
<html>
<head></head>
<body>
<h1>Heading</h1>
<p>Paragraph</p>
<a href=\"http://example.com\">Link</a>
<div class=\"aclass bclass cool\">
<span disabled anotherattr=\"\" thirdthing=\"44\" id=\"attrspan\"
      Capitalized=\"UPPERCASED\">
<div class=\"subdiv cool\" id=\"deepestdiv\">Div</div>
</span>
<!-- Comment 2 -->
<span id=\"anid\" class=\"cool\">Span</span>
</div>
</body>
</html>")

(def html2
  "<!DOCTYPE html>
<html>
<head></head>
<body>
<p>Paragraph 1</p>
<p>Paragraph 2</p>
<p>Paragraph 3</p>
<p>Paragraph 4</p>
<p>Paragraph 5</p>
<p>Paragraph 6</p>
<p>Paragraph 7</p>
<p>Paragraph 8</p>
</body>
</html>")

(deftest select-next-loc-test
  (testing "The select-next-loc function."
    (let [htree (hickory/as-hickory (hickory/parse html1))
          find-comment-fn (fn [zip-loc]
                            (= (:type (zip/node zip-loc))
                               :comment))]
      (let [selection (select/select-next-loc find-comment-fn
                                              (hzip/hickory-zip htree))]
        (is (and (= :comment
                    (-> selection zip/node :type))
                 (re-find #"Comment 1" (-> (zip/node selection)
                                           :content first))))
        (let [second-selection (select/select-next-loc find-comment-fn
                                                       (zip/next selection))]
          (is (and (= :comment
                      (-> second-selection zip/node :type))
                   (re-find #"Comment 2" (-> (zip/node second-selection)
                                             :content first))))
          (is (nil? (select/select-next-loc find-comment-fn
                                            (zip/next second-selection)))))))))

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
      (let [selection (select/select (fn [zip-loc]
                                       (= (:type (zip/node zip-loc))
                                          :comment))
                                     htree)]
        (is (and (= 2 (count selection))
                 (every? true? (map #(= :comment (:type %))
                                    selection))))))))

;;
;; Selector tests
;;

(deftest node-type-test
  (testing "node-type selector"
    (let [htree (hickory/as-hickory (hickory/parse html1))]
      (let [selection (select/select (select/node-type :document-type)
                                     htree)]
        (is (and (= 1 (count selection))
                 (= :document-type (-> selection first :type)))))
      (let [selection (select/select (select/node-type :comment)
                                     htree)]
        (is (and (= 2 (count selection))
                 (every? true? (map #(= :comment (:type %))
                                    selection))))))))

(deftest tag-test
  (testing "tag selector"
    (let [htree (hickory/as-hickory (hickory/parse html1))]
      (let [selection (select/select (select/tag "h1")
                                     htree)]
        (is (and (= 1 (count selection))
                 (= :h1 (-> selection first :tag)))))
      ;; Case-insensitivity test
      (let [selection (select/select (select/tag "H1")
                                     htree)]
        (is (and (= 1 (count selection))
                 (= :h1 (-> selection first :tag)))))
      ;; Non-string argument test
      (let [selection (select/select (select/tag :h1)
                                     htree)]
        (is (and (= 1 (count selection))
                 (= :h1 (-> selection first :tag))))))))

(deftest attr-test
  (testing "attr selector"
    (let [htree (hickory/as-hickory (hickory/parse html1))]
      (let [selection (select/select (select/attr :disabled)
                                     htree)]
        (is (and (= 1 (count selection))
                 (= "attrspan" (-> selection first :attrs :id)))))
      (let [selection (select/select (select/attr "anotherattr")
                                     htree)]
        (is (and (= 1 (count selection))
                 (= "attrspan" (-> selection first :attrs :id)))))
      (let [selection (select/select (select/attr :thirdthing #(= "44" %))
                                     htree)]
        (is (and (= 1 (count selection))
                 (= "attrspan" (-> selection first :attrs :id)))))
      ;; Case-insensitivity of names and non-equality predicate test
      (let [selection (select/select (select/attr "CAPITALIZED"
                                                  #(.startsWith % "UPPER"))
                                     htree)]
        (is (and (= 1 (count selection))
                 (= "attrspan" (-> selection first :attrs :id)))))
      ;; Graceful failure to find anything
      (let [selection (select/select (select/attr "notpresent"
                                                  #(.startsWith % "never"))
                                     htree)]
        (is (= 0 (count selection)))))))

(deftest id-test
  (testing "id selector"
    (let [htree (hickory/as-hickory (hickory/parse html1))]
      (let [selection (select/select (select/id "deepestdiv")
                                     htree)]
        (is (and (= 1 (count selection))
                 (re-find #"deepestdiv"
                          (-> selection first :attrs :id)))))
      (let [selection (select/select (select/id "anid")
                                     htree)]
        (is (and (= 1 (count selection))
                 (re-find #"anid"
                          (-> selection first :attrs :id)))))
      ;; Case-insensitivity test
      (let [selection (select/select (select/id "ANID")
                                     htree)]
        (is (and (= 1 (count selection))
                 (re-find #"anid"
                          (-> selection first :attrs :id)))))
      ;; Non-string argument test
      (let [selection (select/select (select/id :anid)
                                     htree)]
        (is (and (= 1 (count selection))
                 (re-find #"anid"
                          (-> selection first :attrs :id))))))))

(deftest class-test
  (testing "class selector"
    (let [htree (hickory/as-hickory (hickory/parse html1))]
      (let [selection (select/select (select/class "aclass")
                                     htree)]
        (is (and (= 1 (count selection))
                 (re-find #"aclass"
                          (-> selection first :attrs :class)))))
      (let [selection (select/select (select/class "cool")
                                     htree)]
        (is (and (= 3 (count selection))
                 (every? #(not (nil? %))
                         (map #(re-find #"cool"
                                        (-> % :attrs :class))
                              selection)))))
      ;; Case-insensitivity test
      (let [selection (select/select (select/class "Aclass")
                                     htree)]
        (is (and (= 1 (count selection))
                 (re-find #"aclass"
                          (-> selection first :attrs :class)))))
      ;; Non-string argument test
      (let [selection (select/select (select/class :aclass)
                                     htree)]
        (is (and (= 1 (count selection))
                 (re-find #"aclass"
                          (-> selection first :attrs :class))))))))

(deftest any-test
  (testing "any selector"
    (let [htree (hickory/as-hickory (hickory/parse html1))]
      (let [selection (select/select select/any
                                     htree)]
        (is (= 10 (count selection)))))))

(deftest root-test
  (testing "root selector"
    (let [htree (hickory/as-hickory (hickory/parse html1))]
      (let [selection (select/select select/root
                                     htree)]
        (is (= :html (-> selection first :tag)))))))

(deftest n-moves-until-test
  (testing "n-moves-until selector"
    ;; This function is actually pretty well exercised by nth-child, etc.
    (let [htree (hickory/as-hickory (hickory/parse html1))]
      (let [selection (select/select (select/and (select/tag :div)
                                                 (select/n-moves-until 0 6
                                                                       zip/up
                                                                       nil?))
                                     htree)]
        (is (= "deepestdiv" (-> selection first :attrs :id)))))))

(deftest nth-of-type-test
  (testing "nth-of-type selector"
    (let [htree (hickory/as-hickory (hickory/parse html1))]
      (let [selection (select/select (select/nth-of-type 1 :body)
                                     htree)]
        (is (and (= 1 (count selection))
                 (= :body (:tag (first selection)))))))))

(deftest nth-last-of-type-test
  (testing "nth-last-of-type selector"
    (let [htree (hickory/as-hickory (hickory/parse html1))]
      (let [selection (select/select (select/nth-last-of-type 1 :span)
                                     htree)]
        (is (and (= 1 (count selection))
                 (= "anid" (-> selection first :attrs :id))))))))

(deftest nth-child-test
  (testing "nth-child selector"
    (let [htree (hickory/as-hickory (hickory/parse html1))]
      (let [selection (select/select (select/and (select/tag :div)
                                                 (select/nth-child 0 1))
                                     htree)]
        (is (and (= 1 (count selection))
                 (= "deepestdiv" (-> selection first :attrs :id)))))
      (let [selection (select/select (select/and (select/tag :div)
                                                 (select/nth-child 1 1))
                                     htree)]
        (is (and (= 2 (count selection))
                 (every? true? (map #(= :div (:tag %))
                                    selection)))))
      (let [selection (select/select (select/and (select/tag :div)
                                                 (select/nth-child :odd))
                                     htree)]
        (is (and (= 1 (count selection))
                 (= "deepestdiv" (-> selection first :attrs :id)))))
      (let [selection (select/select (select/and (select/node-type :element)
                                                 (select/nth-child :even))
                                     htree)]
        (is (and (= 4 (count selection))
                 (= :element (-> selection first :type))))))
    (let [htree (hickory/as-hickory (hickory/parse html2))]
      (let [selection (select/select (select/and (select/node-type :element)
                                                 (select/nth-child :even))
                                     htree)]
        (is (and (= 5 (count selection))
                 (every? true? (map #(contains? #{:body :p} (:tag %))
                                    selection)))))
      (let [selection (select/select (select/nth-child 3 0)
                                     htree)]
        (is (and (= 2 (count selection))
                 (every? true? (map #(= :p (:tag %))
                                    selection)))))
      (let [selection (select/select (select/child (select/tag :body)
                                                   (select/nth-child 3 1))
                                     htree)]
        (is (and (= 3 (count selection))
                 (every? true? (map #(= :p (:tag %))
                                    selection))))))))

(deftest nth-last-child-test
  (testing "nth-last-child selector"
    (let [htree (hickory/as-hickory (hickory/parse html1))]
      (let [selection (select/select (select/and (select/tag :div)
                                                 (select/nth-last-child 0 1))
                                     htree)]
        (is (and (= 2 (count selection))
                 (every? true? (map #(= :div (:tag %)) selection)))))
      (let [selection (select/select (select/and (select/tag :div)
                                                 (select/nth-last-child 1 1))
                                     htree)]
        (is (and (= 2 (count selection))
                 (every? true? (map #(= :div (:tag %))
                                    selection)))))
      (let [selection (select/select (select/and (select/tag :div)
                                                 (select/nth-last-child :odd))
                                     htree)]
        (is (and (= 2 (count selection))
                 (every? true? (map #(= :div (:tag %)) selection)))))
      (let [selection (select/select (select/and (select/node-type :element)
                                                 (select/nth-last-child :even))
                                     htree)]
        (is (and (= 4 (count selection))
                 (= :element (-> selection first :type))))))
    (let [htree (hickory/as-hickory (hickory/parse html2))]
      (let [selection (select/select (select/and (select/node-type :element)
                                                 (select/nth-last-child :even))
                                     htree)]
        (is (and (= 5 (count selection))
                 (every? true? (map #(contains? #{:head :p} (:tag %))
                                    selection)))))
      (let [selection (select/select (select/nth-last-child 3 0)
                                     htree)]
        (is (and (= 2 (count selection))
                 (every? true? (map #(= :p (:tag %))
                                    selection)))))
      (let [selection (select/select (select/child (select/tag :body)
                                                   (select/nth-last-child 3 1))
                                     htree)]
        (is (and (= 3 (count selection))
                 (every? true? (map #(= :p (:tag %))
                                    selection))))))))

(deftest first-child-test
  (testing "first-child selector"
    (let [htree (hickory/as-hickory (hickory/parse html1))]
      (let [selection (select/select (select/child (select/tag :div)
                                                   select/first-child)
                                     htree)]
        (is (and (= 1 (count selection))
                 (= "attrspan" (-> selection first :attrs :id))))))))

(deftest last-child-test
  (testing "last-child selector"
    (let [htree (hickory/as-hickory (hickory/parse html1))]
      (let [selection (select/select (select/child (select/tag :div)
                                                   select/last-child)
                                     htree)]
        (is (and (= 1 (count selection))
                 (= "anid" (-> selection first :attrs :id))))))))

;;
;; Selector Combinators
;;

(deftest and-test
  (testing "and selector combinator"
    (let [htree (hickory/as-hickory (hickory/parse html1))]
      (let [selection (select/select (select/and (select/tag :div))
                                     htree)]
        (is (and (= 2 (count selection))
                 (every? true? (map #(= :div (:tag %)) selection)))))
      (let [selection (select/select (select/and (select/tag :div)
                                                 (select/class "bclass"))
                                     htree)]
        (is (and (= 1 (count selection))
                 (re-find #"bclass"
                          (-> selection first :attrs :class)))))
      (let [selection (select/select (select/and (select/class "cool")
                                                 (select/tag :span))
                                     htree)]
        (is (and (= 1 (count selection))
                 (= "anid" (-> selection first :attrs :id)))))

      (let [selection (select/select (select/and (select/class "cool")
                                                 (select/tag :span)
                                                 (select/id :attrspan))
                                     htree)]
        (is (= [] selection))))))

(deftest or-test
  (testing "or selector combinator"
    (let [htree (hickory/as-hickory (hickory/parse html1))]
      (let [selection (select/select (select/or (select/tag :a)
                                                (select/class "notpresent")
                                                (select/id :nothere))
                                     htree)]
        (= [] selection))
      (let [selection (select/select (select/or (select/tag :div))
                                     htree)]
        (is (and (= 2 (count selection))
                 (every? true? (map #(= :div (:tag %)) selection)))))
      (let [selection (select/select (select/or (select/id "deepestdiv")
                                                (select/class "bclass"))
                                     htree)]
        (is (and (= 2 (count selection))
                 (every? true? (map #(= :div (:tag %)) selection))))))))

(deftest not-test
  (testing "not selector combinator"
    (let [htree (hickory/as-hickory (hickory/parse html1))]
      (let [selection (select/select (select/and (select/node-type :element)
                                                 (select/not (select/class :cool)))
                                     htree)]
        (is (and (= 7 (count selection))
                 (every? true? (map #(and (= :element (-> % :type))
                                          (or (not (-> % :attrs :class))
                                              (not (re-find #"cool"
                                                            (-> % :attrs :class)))))
                                    selection)))))
      (let [selection (select/select (select/el-not (select/class :cool))
                                     htree)]
        (is (and (= 7 (count selection))
                 (every? true? (map #(and (= :element (-> % :type))
                                          (or (not (-> % :attrs :class))
                                              (not (re-find #"cool"
                                                            (-> % :attrs :class)))))
                                    selection)))))
      (let [selection (select/select (select/not (select/class :cool))
                                     htree)]
        (is (= 31 (count selection)))))))

(deftest child-test
  (testing "child selector combinator"
    (let [htree (hickory/as-hickory (hickory/parse html1))]
      (let [selection (select/select (select/child (select/el-not select/any))
                                     htree)]
        (is (= [] selection)))
      (let [selection (select/select (select/child (select/tag :html)
                                                   (select/tag :div)
                                                   (select/tag :span))
                                     htree)]
        (is (= [] selection)))
      (let [selection (select/select (select/child (select/tag :body)
                                                   (select/tag :div)
                                                   (select/tag :span))
                                     htree)]
        (is (and (= 2 (count selection))
                 (every? true? (map #(= :span (:tag %)) selection)))))
      (let [selection (select/select (select/child (select/tag :div)
                                                   select/any)
                                     htree)]
        (is (and (= 2 (count selection))
                 (every? true? (map #(or (= :span (-> % :tag))
                                         (= :div (-> % :tag)))
                                    selection))))))
    ;; Check examples from the doc string.
    (let [htree (-> "<div><span class=\"foo\"><input disabled></input></span></div>"
                    hickory/parse hickory/as-hickory)]
      (let [selection (select/select (select/child (select/tag :div)
                                                   (select/class :foo)
                                                   (select/attr :disabled))
                                     htree)]
        (is (= :input (-> selection first :tag)))))
    (let [htree (-> "<div><span class=\"foo\"><b><input disabled></input></b></span></div>"
                    hickory/parse hickory/as-hickory)]
      (let [selection (select/select (select/child (select/tag :div)
                                                   (select/class :foo)
                                                   (select/attr :disabled))
                                     htree)]
        (is (= [] selection))))))

(deftest descendant-test
  (testing "descendant selector combinator"
    (let [htree (hickory/as-hickory (hickory/parse html1))]
      (let [selection (select/select (select/descendant (select/tag :h1))
                                     htree)]
        (is (and (= 1 (count selection))
                 (= :h1 (-> selection first :tag)))))
      (let [selection (select/select (select/descendant (select/class "cool")
                                                        (select/tag :div))
                                     htree)]
        (is (= 1 (count selection))
            (= "deepestdiv" (-> selection first :attrs :id))))
      (let [selection (select/select (select/descendant (select/tag :div)
                                                        select/any)
                                     htree)]
        (is (= 3 (count selection)))))
    ;; Check examples from doc string.
    (let [htree (-> "<div><span class=\"foo\"><input disabled></input></span></div>"
                    hickory/parse hickory/as-hickory)]
      (let [selection (select/select (select/descendant (select/tag :div)
                                                        (select/class :foo)
                                                        (select/attr :disabled))
                                     htree)]
        (is (and (= 1 (count selection))
                 (= :input (-> selection first :tag))))))
    (let [htree (-> "<div><span class=\"foo\"><b><input disabled></input></b></span></div>"
                    hickory/parse hickory/as-hickory)]
      (let [selection (select/select (select/descendant (select/tag :div)
                                                        (select/class :foo)
                                                        (select/attr :disabled))
                                     htree)]
        (is (and (= 1 (count selection))
                 (= :input (-> selection first :tag))))))))
