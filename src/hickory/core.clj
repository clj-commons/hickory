(ns hickory.core
  (:require [clojure.string :as string])
  (:import [org.jsoup Jsoup]
           [org.jsoup.nodes Attribute Attributes Comment Document
            DocumentType Element Node TextNode XmlDeclaration]
           [org.jsoup.parser Tag]))

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
  [s]
  (as-hiccup (Jsoup/parse s)))

(defn parse-fragment
  [s]
  (as-hiccup (Jsoup/parseBodyFragment s)))