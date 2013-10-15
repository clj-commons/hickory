(ns hickory.utils
  "Miscellaneous utilities used internally."
  #+clj (:require [quoin.text :as qt])
  (:require [clojure.string :as string]
            #+cljs [goog.string :as gstring]
            #+cljs [goog.string.format]))

;;
;; Data
;;

(def void-element
  "Elements that don't have a meaningful <tag></tag> form."
  #{:area :base :br :col :command :embed :hr :img :input :keygen :link :meta
    :param :source :track :wbr})

(def unescapable-content
  "Elements whose content should never have html-escape codes."
  #{:script :style})

;;
;; String utils
;;

#+cljs
(defn format
  "Formats a string using goog.string.format."
  [fmt & args]
  (apply gstring/format fmt args))

(defn html-escape
  [s]
  #+clj (qt/html-escape s)
  #+cljs (gstring/htmlEscape s))

(defn starts-with
  [^String s ^String prefix]
  #+clj (.startsWith s prefix)
  #+cljs (goog.string.startsWith s prefix))

(defn lower-case-keyword
  "Converts its string argument into a lowercase keyword."
  [s]
  (-> s string/lower-case keyword))

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
