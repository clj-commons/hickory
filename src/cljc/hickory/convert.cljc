(ns hickory.convert
  "Functions to convert from one representation to another."
  (:require [hickory.render :as render]
            [hickory.core :as core]
            [hickory.utils :as utils]))

(defn hiccup-to-hickory
  "Given a sequence of hiccup forms representing a full document,
   returns an equivalent hickory node representation of that document.
   This will perform HTML5 parsing as a full document, no matter what
   it is given.

   Note that this function is heavyweight: it requires a full HTML
   re-parse to work."
  [hiccup-forms]
  (core/as-hickory (core/parse (render/hiccup-to-html hiccup-forms))))

(defn hiccup-fragment-to-hickory
  "Given a sequence of hiccup forms representing a document fragment,
   returns an equivalent sequence of hickory fragments.

   Note that this function is heavyweight: it requires a full HTML
   re-parse to work."
  [hiccup-forms]
  (map core/as-hickory
       (core/parse-fragment (render/hiccup-to-html hiccup-forms))))

(defn hickory-to-hiccup
  "Given a hickory format dom object, returns an equivalent hiccup
   representation. This can be done directly and exactly, but in general
   you will not be able to go back from the hiccup."
  [dom]
  (if (string? dom)
    (utils/html-escape dom)
    (case (:type dom)
      :document
      (mapv hickory-to-hiccup (:content dom))
      :document-type
      (utils/render-doctype (get-in dom [:attrs :name])
                            (get-in dom [:attrs :publicid])
                            (get-in dom [:attrs :systemid]))
      :element
      (if (utils/unescapable-content (:tag dom))
        (if (every? string? (:content dom))
          ;; Merge :attrs contents with {} to prevent nil from getting into
          ;; the hiccup forms when there are no attributes.
          (apply vector (:tag dom) (into {} (:attrs dom)) (:content dom))
          (throw (ex-info
                  "An unescapable content tag had non-string children."
                  {:error-location dom})))
        (apply vector (:tag dom) (into {} (:attrs dom))
               (map hickory-to-hiccup (:content dom))))
      :comment
      (str "<!--" (apply str (:content dom)) "-->"))))

