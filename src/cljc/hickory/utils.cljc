(ns hickory.utils
  "Miscellaneous utilities used internally."
  (:require [clojure.string :as string]
            #?(:cljs [goog.string :as gstring])))

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

(defn clj-html-escape-without-quoin
  "Actually copy pasted from quoin: https://github.com/davidsantiago/quoin/blob/develop/src/quoin/text.clj"
  [^String s]
  ;; This method is "Java in Clojure" for serious speedups.
  (let [sb (StringBuilder.)
        slength (long (count s))]
    (loop [idx (long 0)]
      (if (>= idx slength)
        (.toString sb)
        (let [c (char (.charAt s idx))]
          (case c
            \& (.append sb "&amp;")
            \< (.append sb "&lt;")
            \> (.append sb "&gt;")
            \" (.append sb "&quot;")
            (.append sb c))
          (recur (inc idx)))))))

(defn html-escape
  [s]
  #?(:clj  (clj-html-escape-without-quoin s)
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
