(ns hickory.utils
  "Miscellaneous utilities used internally."
  (:require [clojure.string :as string]
    #?(:clj
            [quoin.text :as qt]
       :cljs [goog.string :as gstring])))

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

(defn html-escape
  [s]
  #?(:clj  (qt/html-escape s)
     :cljs (gstring/htmlEscape s)))

(defn starts-with
  [^String s ^String prefix]
  #?(:clj  (.startsWith s prefix)
     :cljs (goog.string.startsWith s prefix)))

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
