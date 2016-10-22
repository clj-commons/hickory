(ns hickory.core
  (:require [hickory.utils :as utils]
            [clojure.zip :as zip]
            [goog.string :as gstring]))

;;
;; Protocols
;;

(defprotocol HiccupRepresentable
  "Objects that can be represented as Hiccup nodes implement this protocol in
   order to make the conversion."
  (as-hiccup [this]
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

(defn node-type
  [type]
  (aget js/Node (str type "_NODE")))

(def Attribute (node-type "ATTRIBUTE"))
(def Comment (node-type "COMMENT"))
(def Document (node-type "DOCUMENT"))
(def DocumentType (node-type "DOCUMENT_TYPE"))
(def Element (node-type "ELEMENT"))
(def Text (node-type "TEXT"))

(defn extend-type-with-seqable
  [t]
  (extend-type t
    ISeqable
    (-seq [array] (array-seq array))))

(extend-type-with-seqable js/NodeList)

(when (exists? js/NamedNodeMap)
  (extend-type-with-seqable js/NamedNodeMap))

(when (exists? js/MozNamedAttrMap) ;;NamedNodeMap has been renamed on modern gecko implementations (see https://developer.mozilla.org/en-US/docs/Web/API/NamedNodeMap)
  (extend-type-with-seqable js/MozNamedAttrMap))

(defn format-doctype
  [dt]
  (let [name (aget dt "name")
        publicId (aget dt "publicId")
        systemId (aget dt "systemId")]
    (if (not (empty? publicId))
      (gstring/format "<!DOCTYPE %s PUBLIC \"%s\" \"%s\">" name publicId systemId)
      (str "<!DOCTYPE " name ">"))))

(extend-protocol HiccupRepresentable
  object
  (as-hiccup [this] (condp = (aget this "nodeType")
                      Attribute [(utils/lower-case-keyword (aget this "name"))
                                 (aget this "value")]
                      Comment (str "<!--" (aget this "data") "-->")
                      Document (map as-hiccup (aget this "childNodes"))
                      DocumentType (format-doctype this)
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
                      Element (let [tag (utils/lower-case-keyword (aget this "tagName"))]
                                (into [] (concat [tag
                                                  (into {} (map as-hiccup (aget this "attributes")))]
                                                 (if (utils/unescapable-content tag)
                                                   (map #(aget % "wholeText") (aget this "childNodes"))
                                                   (map as-hiccup (aget this "childNodes"))))))
                      Text (utils/html-escape (aget this "wholeText")))))

(extend-protocol HickoryRepresentable
  object
  (as-hickory [this] (condp = (aget this "nodeType")
                       Attribute [(utils/lower-case-keyword (aget this "name")) (aget this "value")]
                       Comment {:type :comment
                                :content [(aget this "data")]}
                       Document {:type :document
                                 :content (not-empty
                                            (into [] (map as-hickory
                                                          (aget this "childNodes"))))}
                       DocumentType {:type :document-type
                                     :attrs {:name (aget this "name")
                                             :publicid (aget this "publicId")
                                             :systemid (aget this "systemId")}}
                       Element {:type :element
                                :attrs (not-empty (into {} (map as-hickory (aget this "attributes"))))
                                :tag (utils/lower-case-keyword (aget this "tagName"))
                                :content (not-empty
                                           (into [] (map as-hickory
                                                         (aget this "childNodes"))))}
                       Text (aget this "wholeText"))))

(defn extract-doctype
  [s]
  ;;Starting HTML5 doctype definition can be uppercase
  (when-let [doctype (second (or (re-find #"<!DOCTYPE ([^>]*)>" s)
                                 (re-find #"<!doctype ([^>]*)>" s)))]
    (re-find #"([^\s]*)(\s+PUBLIC\s+[\"]?([^\"]*)[\"]?\s+[\"]?([^\"]*)[\"]?)?" doctype)))

(defn remove-el
  [el]
  (.removeChild (aget el "parentNode") el))

(defn parse-dom-with-domparser
  [s]
  (if (exists? js/DOMParser)
    (.parseFromString (js/DOMParser.) s "text/html")))

(defn parse-dom-with-write
  "Parse an HTML document (or fragment) as a DOM using document.implementation.createHTMLDocument and document.write."
  [s]
  ;;See http://www.w3.org/TR/domcore/#dom-domimplementation-createhtmldocument for more details.
  (let [doc (.createHTMLDocument js/document.implementation "") ;;empty title for older implementation
        doctype-el (aget doc "doctype")]
    (when-not (extract-doctype s);; Remove default doctype if parsed string does not define it.
      (remove-el doctype-el))
    (when-let [title-el (first (aget doc "head" "childNodes"))];; Remove default title if parsed string does not define it.
      (when (empty? (aget title-el "text"))
          (remove-el title-el)))
    (.write doc s)
    doc))

(defn parse
  "Parse an entire HTML document into a DOM structure that can be
   used as input to as-hiccup or as-hickory."
  [s]
  (or (parse-dom-with-domparser s) (parse-dom-with-write s)))

(defn parse-fragment
  "Parse an HTML fragment (some group of tags that might be at home somewhere
   in the tag hierarchy under <body>) into a list of DOM elements that can
   each be passed as input to as-hiccup or as-hickory."
  [s]
  (aget (parse s) "body" "childNodes"))
