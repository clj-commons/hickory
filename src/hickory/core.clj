(ns hickory.core
  (:require [clojure.string :as string])
  (:import [org.jsoup Jsoup]
           [org.jsoup.nodes Attribute Attributes Comment DataNode Document
            DocumentType Element Node TextNode XmlDeclaration]
           [org.jsoup.parser Tag Parser]))

(defn lower-case-keyword
  [s]
  (-> s string/lower-case keyword))

(defprotocol HiccupRepresentable
  "Objects that can be represented as Hiccup nodes implement this protocol in
   order to make the conversion."
  (as-hiccup [this]))

(extend-protocol HiccupRepresentable
  Attribute
  (as-hiccup [this] [(lower-case-keyword (.getKey this))
                     (.getValue this)])
  Attributes
  (as-hiccup [this] (into {} (map as-hiccup this)))
  Comment
  (as-hiccup [this] (str this))
  DataNode
  (as-hiccup [this] (str this))
  Document
  (as-hiccup [this] (into [] (map as-hiccup (.childNodes this))))
  DocumentType
  (as-hiccup [this] (str this))
  Element
  (as-hiccup [this] (into [] (concat [(as-hiccup (.tag this))
                                      (as-hiccup (.attributes this))]
                                     (map as-hiccup (.childNodes this)))))
  Tag
  (as-hiccup [this] (lower-case-keyword (.getName this)))
  TextNode
  (as-hiccup [this] (.text this))
  XmlDeclaration
  (as-hiccup [this] (str this)))

(defn parse
  "Parse an entire HTML document into hiccup vectors."
  [s]
  (as-hiccup (Jsoup/parse s)))

(defn parse-fragment
  "Parse an HTML fragment (some group of tags that might be at home somewhere
   in the tag hierarchy under <body>) into hiccup vectors."
  [s]
  ;; Oh god. Oh God. This. is. *heinous*.
  ;;
  ;; Jsoup, excellent parser, has a bug in Parser/parseFragment when you do not
  ;; supply the context arg. It has an uninitialized variable that causes an
  ;; NPE. So, I found this problem and reported it. Haven't heard back. Project
  ;; won't build for me, and I can't figure out why.
  ;;
  ;; So, after hours spent on this, it's time to make a strategic retreat to
  ;; awfulness, and use the function that does work: parseBodyFragment. This
  ;; parse the fragment into an entire html/head/body structure. We'll let it
  ;; do that, and then *gulp* find the thing we wanted in that body and pull it
  ;; back out.
  ;;
  ;; Seriously, soon as they fix parseFragment, fix this. The code should be
  ;; (as-hiccup (Parser/parseFragment s nil "")) or something close.
  (into []
   (rest (rest (get-in (as-hiccup (Parser/parseBodyFragment s ""))
                       [0 3])))))