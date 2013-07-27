(ns hickory.utils
  "Miscellaneous utilities used internally."
  #+clj (:require [quoin.text :as qt])
  #+cljs (:require [goog.string :as gstring]))

(defn starts-with
  [^String s ^String prefix]
  #+clj (.startsWith s prefix)
  #+cljs (goog.string.startsWith s prefix))

(def void-element
  "Elements that don't have a meaningful <tag></tag> form."
  #{:area :base :br :col :command :embed :hr :img :input :keygen :link :meta
    :param :source :track :wbr})

(defn html-escape
  [s]
  #+clj (qt/html-escape s)
  #+cljs (gstring/htmlEscape s))

(def unescapable-content
  "Elements whose content should never have html-escape codes."
  #{:script :style})

(defn render-doctype
  "Returns a string containing the HTML source for the doctype with given args.
   The second and third arguments can be nil or empty strings."
  [name publicid systemid]
  (str "<!DOCTYPE " name
       (when (not-empty publicid)
         (str " PUBLIC \"" publicid "\""))
       (when (not-empty systemid)
         (str " \"" systemid "\""))
       ">"))
