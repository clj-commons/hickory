(ns hickory.core
  (:require [hickory.utils :as utils]
            [clojure.zip :as zip])
  (:import [org.jsoup Jsoup]
           [org.jsoup.nodes Attribute Attributes Comment DataNode Document
            DocumentType Element Node TextNode XmlDeclaration]
           [org.jsoup.parser Tag Parser]))

;;
;; Protocols
;;

(defprotocol HiccupRepresentable
  "Objects that can be represented as Hiccup nodes implement this protocol in
   order to make the conversion."
  (as-hiccup [this escape]
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
  (as-hickory [this]
    "Converts the node given into a hickory-format data structure. The
     node must have an implementation of the HickoryRepresentable protocol;
     nodes created by parse or parse-fragment already do."))


(extend-protocol HiccupRepresentable
  Attribute
  ;; Note the attribute value is not html-escaped; see comment for Element.
  (as-hiccup [this escape] [(utils/lower-case-keyword (.getKey this))
                     (.getValue this)])
  Attributes
  (as-hiccup [this escape] (into {} (map #(as-hiccup % escape) this)))
  Comment
  (as-hiccup [this escape] (str "<!--" (.getData this) "-->"))
  DataNode
  (as-hiccup [this escape] (str this))
  Document
  (as-hiccup [this escape] (map #(as-hiccup % escape) (.childNodes this)))
  DocumentType
  (as-hiccup [this escape] (str this))
  Element
  (as-hiccup [this escape]
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
    (let [tag (utils/lower-case-keyword (.tagName this))]
      (into [] (concat [tag
                        (as-hiccup (.attributes this) escape)]
                       (if (utils/unescapable-content tag)
                         (map str (.childNodes this))
                         (map #(as-hiccup % escape) (.childNodes this)))))))
  TextNode
  ;; See comment for Element re: html escaping.
  (as-hiccup [this escape]
    (let [unescaped (.getWholeText this)]
      (if escape
        (utils/html-escape unescaped)
        unescaped)))
  XmlDeclaration
  (as-hiccup [this escape] (str this)))

(extend-protocol HickoryRepresentable
  Attribute
  (as-hickory [this] [(utils/lower-case-keyword (.getKey this))
                      (.getValue this)])
  Attributes
  (as-hickory [this] (not-empty (into {} (map as-hickory this))))
  Comment
  (as-hickory [this] {:type :comment
                      :content [(.getData this)]})
  DataNode
  (as-hickory [this] (str this))
  Document
  (as-hickory [this] {:type :document
                      :content (not-empty
                                (into [] (map as-hickory
                                              (.childNodes this))))})
  DocumentType
  (as-hickory [this] {:type :document-type
                      :attrs (as-hickory (.attributes this))})
  Element
  (as-hickory [this] {:type :element
                      :attrs (as-hickory (.attributes this))
                      :tag (utils/lower-case-keyword (.tagName this))
                      :content (not-empty
                                (into [] (map as-hickory
                                              (.childNodes this))))})
  TextNode
  (as-hickory [this] (.getWholeText this)))

(defn parse
  "Parse an entire HTML document into a DOM structure that can be
   used as input to as-hiccup or as-hickory."
  [s]
  (Jsoup/parse s))

(defn parse-fragment
  "Parse an HTML fragment (some group of tags that might be at home somewhere
   in the tag hierarchy under <body>) into a list of DOM elements that can
   each be passed as input to as-hiccup or as-hickory."
  [s]
  (into [] (Parser/parseFragment s (Element. (Tag/valueOf "body") "") "")))
