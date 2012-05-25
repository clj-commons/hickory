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

(defprotocol HTMLDOMMapRepresentable
  "Objects that can be represented as HTML DOM node maps, similar to
   clojure.xml, implement this protocol to make the conversion.

   Each DOM node will be a map or string (for Text/CDATASections). Nodes that
   are maps have the appropriate subset of the keys

     :type     - [:comment, :document, :document-type, :element]
     :tag      - node's tag, check :type to see if applicable
     :attrs    - node's attributes as a map, check :type to see if applicable
     :children - node's child nodes, in a vector, check :type to see if
                 applicable"
  (as-dom-map [this]))


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
  (as-hiccup [this] (into [] (concat [(lower-case-keyword (.tagName this))
                                      (as-hiccup (.attributes this))]
                                     (map as-hiccup (.childNodes this)))))
  TextNode
  (as-hiccup [this] (.text this))
  XmlDeclaration
  (as-hiccup [this] (str this)))

(extend-protocol HTMLDOMMapRepresentable
  Attribute
  (as-dom-map [this] [(lower-case-keyword (.getKey this))
                      (.getValue this)])
  Attributes
  (as-dom-map [this] (into {} (map as-dom-map this)))
  Comment
  (as-dom-map [this] {:type :comment
                      :children [(.getData this)]})
  DataNode
  (as-dom-map [this] (str this))
  Document
  (as-dom-map [this] {:type :document
                      :children (into [] (map as-dom-map (.childNodes this)))})
  DocumentType
  (as-dom-map [this] {:type :document-type
                      :attrs (as-dom-map (.attributes this))})
  Element
  (as-dom-map [this] {:type :element
                      :attrs (as-dom-map (.attributes this))
                      :tag (lower-case-keyword (.tagName this))
                      :children (into [] (map as-dom-map (.childNodes this)))})
  TextNode
  (as-dom-map [this] (.text this)))

(defn parse
  "Parse an entire HTML document into a DOM structure that can be
   used as input to as-hiccup or as-dom-map."
  [s]
  (Jsoup/parse s))

(defn parse-fragment
  "Parse an HTML fragment (some group of tags that might be at home somewhere
   in the tag hierarchy under <body>) into a list of DOM elements that can
   each be passed as input to as-hiccup or as-dom-map."
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
  ;; (Parser/parseFragment s nil "") or something close.
  (into [] (.. (Parser/parseBodyFragment s "")
               childNodes (get 0) ;; <html> tag
               childNodes (get 1) ;; <body> tag
               childNodes)))      ;; contents of <body> tag
