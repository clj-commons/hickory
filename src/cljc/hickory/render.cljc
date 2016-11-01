(ns hickory.render
  (:require [hickory.hiccup-utils :as hu]
            [hickory.utils :as utils]
            [clojure.string :as str]))

;;
;; Hickory to HTML
;;

(defn- render-hickory-attribute
  "Given a map entry m, representing the attribute name and value, returns a
   string representing that key/value pair as it would be rendered into HTML."
  [m]
  (str " " (name (key m)) "=\"" (utils/html-escape (val m)) "\""))

(defn hickory-to-html
  "Given a hickory HTML DOM map structure (as returned by as-hickory), returns a
   string containing HTML it represents. Keep in mind this function is not super
   fast or heavy-duty.

   Note that it will NOT in general be the case that

     (= my-html-src (hickory-to-html (as-hickory (parse my-html-src))))

   as we do not keep any letter case or whitespace information, any
   \"tag-soupy\" elements, attribute quote characters used, etc."
  [dom]
  (if (string? dom)
    (utils/html-escape dom)
    (try
      (case (:type dom)
        :document
        (apply str (map hickory-to-html (:content dom)))
        :document-type
        (utils/render-doctype (get-in dom [:attrs :name])
                              (get-in dom [:attrs :publicid])
                              (get-in dom [:attrs :systemid]))
        :element
        (cond
         (utils/void-element (:tag dom))
         (str "<" (name (:tag dom))
              (apply str (map render-hickory-attribute (:attrs dom)))
              ">")
         (utils/unescapable-content (:tag dom))
         (str "<" (name (:tag dom))
              (apply str (map render-hickory-attribute (:attrs dom)))
              ">"
              (apply str (:content dom)) ;; Won't get html-escaped.
              "</" (name (:tag dom)) ">")
         :else
         (str "<" (name (:tag dom))
              (apply str (map render-hickory-attribute (:attrs dom)))
              ">"
              (apply str (map hickory-to-html (:content dom)))
              "</" (name (:tag dom)) ">"))
        :comment
        (str "<!--" (apply str (:content dom)) "-->"))
      (catch #?(:clj  IllegalArgumentException
                :cljs js/Error) e
        (throw
          (if (utils/starts-with #?(:clj (.getMessage e) :cljs (aget e "message")) "No matching clause: ")
            (ex-info (str "Not a valid node: " (pr-str dom)) {:dom dom})
            e))))))

;;
;; Hiccup to HTML
;;

(defn- render-hiccup-attrs
  "Given a hiccup attribute map, returns a string containing the attributes
   rendered as they should appear in an HTML tag, right after the tag (including
   a leading space to separate from the tag, if any attributes present)."
  [attrs]
  ;; Hiccup normally does not html-escape strings, but it does for attribute
  ;; values.
  (let [attrs-str (->> (for [[k v] attrs]
                         (cond (true? v)
                               (str (name k))
                               (nil? v)
                               ""
                               :else
                               (str (name k) "=" "\"" (utils/html-escape v) "\"")))
                       (filter #(not (empty? %)))
                       sort
                       (str/join " "))]
    (if (not (empty? attrs-str))
      ;; If the attrs-str is not "", we need to pad the front so that the
      ;; tag will separate from the attributes. Otherwise, "" is fine to return.
      (str " " attrs-str)
      attrs-str)))

(declare hiccup-to-html)
(defn- render-hiccup-element
  "Given a normalized hiccup element (such as the output of
   hickory.hiccup-utils/normalize-form; see this function's docstring
   for more detailed definition of a normalized hiccup element), renders
   it to HTML and returns it as a string."
  [n-element]
  (let [[tag attrs & content] n-element]
    (if (utils/void-element tag)
      (str "<" (name tag) (render-hiccup-attrs attrs) ">")
      (str "<" (name tag) (render-hiccup-attrs attrs) ">"
           (hiccup-to-html content)
           "</" (name tag) ">"))))

(defn- render-hiccup-form
  "Given a normalized hiccup form (such as the output of
   hickory.hiccup-utils/normalize-form; see this function's docstring
   for more detailed definition of a normalized hiccup form), renders
   it to HTML and returns it as a string."
  [n-form]
  (if (vector? n-form)
    (render-hiccup-element n-form)
    n-form))

(defn hiccup-to-html
  "Given a sequence of hiccup forms (as returned by as-hiccup), returns a
   string containing HTML it represents. Keep in mind this function is not super
   fast or heavy-duty, and definitely not a replacement for dedicated hiccup
   renderers, like hiccup itself, which *is* fast and heavy-duty.

```klipse
  (hiccup-to-html '([:html {} [:head {}] [:body {} [:a {} \"foo\"]]]))
```

   Note that it will NOT in general be the case that

     (= my-html-src (hiccup-to-html (as-hiccup (parse my-html-src))))

   as we do not keep any letter case or whitespace information, any
   \"tag-soupy\" elements, attribute quote characters used, etc. It will also
   not generally be the case that this function's output will exactly match
   hiccup's.
   For instance:

```klipse
(hiccup-to-html (as-hiccup (parse \"<A href=\\\"foo\\\">foo</A>\")))
```
  "
  [hiccup-forms]
  (apply str (map #(render-hiccup-form (hu/normalize-form %)) hiccup-forms)))

