(ns hickory.convert
  "Functions to convert from one representation to another."
  (:require [hickory.render :as render]
            [hickory.core :as core]))

(defn hiccup-to-hickory
  "Given a sequence of hiccup forms representing a document,
   returns an equivalent hickory representation. It will parse
   as a complete HTML document.

   Note that this function is heavyweight: it requires a full HTML
   re-parse to work."
  [hiccup-forms]
  (core/as-hickory (core/parse (render/hiccup-to-html hiccup-forms))))

