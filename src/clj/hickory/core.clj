(ns hickory.core
  (:require [hickory.utils :as utils]
            [hickory.zip :as hzip]
            [clojure.zip :as zip])
  (:import [org.jsoup Jsoup]
           [org.jsoup.nodes Attribute Attributes Comment DataNode Document
            DocumentType Element TextNode XmlDeclaration]
           [org.jsoup.parser Tag Parser]))

(set! *warn-on-reflection* true)

(defn- end-or-recur [as-fn loc data & [skip-child?]]
  (let [new-loc (-> loc (zip/replace data) zip/next (cond-> skip-child? zip/next))]
    (if (zip/end? new-loc)
      (zip/root new-loc)
      #(as-fn (zip/node new-loc) new-loc))))

;;
;; Protocols
;;

(defprotocol HiccupRepresentable
  "Objects that can be represented as Hiccup nodes implement this protocol in
   order to make the conversion."
  (as-hiccup [this] [this zip-loc]
    "Converts the node given into a hiccup-format data structure. The
     node must have an implementation of the HiccupRepresentable
     protocol; nodes created by parse or parse-fragment already do."))

(defprotocol HickoryRepresentable
  "Objects that can be represented as HTML DOM node maps, similar to
   clojure.xml, implement this protocol to make the conversion.

   Each DOM node will be a map or string (for Text/CDATASections). Nodes that
   are maps have the appropriate subset of the keys

     :type     - [:comment, :document, :document-type, :element]
     :tag      - node's tag, check :type to see if applicable
     :attrs    - node's attributes as a map, check :type to see if applicable
     :content  - node's child nodes, in a vector, check :type to see if
                 applicable"
  (as-hickory [this] [this zip-loc]
    "Converts the node given into a hickory-format data structure. The
     node must have an implementation of the HickoryRepresentable protocol;
     nodes created by parse or parse-fragment already do."))


(extend-protocol HiccupRepresentable
  Attribute
  ;; Note the attribute value is not html-escaped; see comment for Element.
  (as-hiccup
    ([this] (trampoline as-hiccup this (hzip/hiccup-zip this)))
    ([this _] [(utils/lower-case-keyword (.getKey this)) (.getValue this)]))
  Attributes
  (as-hiccup
    ([this] (trampoline as-hiccup this (hzip/hiccup-zip this)))
    ([this _] (into {} (map as-hiccup this))))
  Comment
  (as-hiccup
    ([this] (trampoline as-hiccup this (hzip/hiccup-zip this)))
    ([this loc] (end-or-recur as-hiccup loc (str "<!--" (.getData this) "-->"))))
  DataNode
  (as-hiccup
    ([this] (trampoline as-hiccup this (hzip/hiccup-zip this)))
    ([this loc] (end-or-recur as-hiccup loc (str this))))
  Document
  (as-hiccup
    ([this] (trampoline as-hiccup this (hzip/hiccup-zip this)))
    ([this loc] (end-or-recur as-hiccup loc (apply list (.childNodes this)))))
  DocumentType
  (as-hiccup
    ([this] (trampoline as-hiccup this (hzip/hiccup-zip this)))
    ([this loc]
     (end-or-recur as-hiccup loc (utils/render-doctype (.name this)
                                                       (.publicId this)
                                                       (.systemId this)))))
  Element
  (as-hiccup
    ([this] (trampoline as-hiccup this (hzip/hiccup-zip this)))
    ([this loc]
     ;; There is an issue with the hiccup format, which is that it
     ;; can't quite cover all the pieces of HTML, so anything it
     ;; doesn't cover is thrown into a string containing the raw
     ;; HTML. This presents a problem because it is then never the case
     ;; that a string in a hiccup form should be html-escaped (except
     ;; in an attribute value) when rendering; it should already have
     ;; any escaping. Since the HTML parser quite properly un-escapes
     ;; HTML where it should, we have to go back and un-un-escape it
     ;; wherever text would have been un-escaped. We do this by
     ;; html-escaping the parsed contents of text nodes, and not
     ;; html-escaping comments, data-nodes, and the contents of
     ;; unescapable nodes.
     (let [tag (utils/lower-case-keyword (.tagName this))
           children (cond->> (.childNodes this) (utils/unescapable-content tag) (map str))
           data (into [] (concat [tag (trampoline as-hiccup (.attributes this))] children))]
       (end-or-recur as-hiccup loc data (utils/unescapable-content tag)))))
  TextNode
  ;; See comment for Element re: html escaping.
  (as-hiccup
    ([this] (trampoline as-hiccup this (hzip/hiccup-zip this)))
    ([this loc] (end-or-recur as-hiccup loc (utils/html-escape (.getWholeText this)))))
  XmlDeclaration
  (as-hiccup
    ([this] (trampoline as-hiccup this (hzip/hiccup-zip this)))
    ([this loc] (end-or-recur as-hiccup loc (str this)))))

(extend-protocol HickoryRepresentable
  Attribute
  (as-hickory
    ([this] (trampoline as-hickory this (hzip/hickory-zip this)))
    ([this _] [(utils/lower-case-keyword (.getKey this)) (.getValue this)]))
  Attributes
  (as-hickory
    ([this] (trampoline as-hickory this (hzip/hickory-zip this)))
    ([this _] (not-empty (into {} (map as-hickory this)))))
  Comment
  (as-hickory
    ([this] (trampoline as-hickory this (hzip/hickory-zip this)))
    ([this loc] (end-or-recur as-hickory loc {:type :comment
                                              :content [(.getData this)]} true)))
  DataNode
  (as-hickory
    ([this] (trampoline as-hickory this (hzip/hickory-zip this)))
    ([this loc] (end-or-recur as-hickory loc (str this))))
  Document
  (as-hickory
    ([this] (trampoline as-hickory this (hzip/hickory-zip this)))
    ([this loc] (end-or-recur as-hickory loc {:type :document
                                              :content (or (seq (.childNodes this)) nil)})))
  DocumentType
  (as-hickory
    ([this] (trampoline as-hickory this (hzip/hickory-zip this)))
    ([this loc] (end-or-recur as-hickory loc {:type :document-type
                                              :attrs (trampoline as-hickory (.attributes this))})))
  Element
  (as-hickory
    ([this] (trampoline as-hickory this (hzip/hickory-zip this)))
    ([this loc] (end-or-recur as-hickory loc {:type :element
                                              :attrs (trampoline as-hickory (.attributes this))
                                              :tag (utils/lower-case-keyword (.tagName this))
                                              :content (or (seq (.childNodes this)) nil)})))
  TextNode
  (as-hickory
    ([this] (trampoline as-hickory this (hzip/hickory-zip this)))
    ([this loc] (end-or-recur as-hickory loc (.getWholeText this)))))

(defn parse
  "Parse an entire HTML document into a DOM structure that can be
   used as input to as-hiccup or as-hickory."
  [s]
  (cond (instance? String s)
        (Jsoup/parse ^String s)
        (instance? java.io.File s)
        (Jsoup/parse ^java.io.File s)
        (instance? java.nio.file.Path s)
        (Jsoup/parse ^java.nio.file.Path s)
        :else
        (throw (ex-info "Invalid input for parse" {:type (type s)}))))

(defn parse-fragment
  "Parse an HTML fragment (some group of tags that might be at home somewhere
   in the tag hierarchy under <body>) into a list of DOM elements that can
   each be passed as input to as-hiccup or as-hickory."
  [s]
  (into [] (Parser/parseFragment s (Element. (Tag/valueOf "body") "") "")))
