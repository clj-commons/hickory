(ns hickory.core
  (:require [clojure.string :as string]
            [clojure.zip :as zip]
            #+cljs [goog.string :as gstring]
            #+cljs [goog.string.StringBuffer :as gbuffer])
  #+clj (:import [org.jsoup Jsoup]
                 [org.jsoup.nodes Attribute Attributes Comment DataNode Document
                  DocumentType Element Node TextNode XmlDeclaration]
                 [org.jsoup.parser Tag Parser]))

;;
;; Utilities
;;
(defn- lower-case-keyword
  "Converts its string argument into a lowercase keyword."
  [s]
  (-> s string/lower-case keyword))

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

#+clj
(extend-protocol HiccupRepresentable
  Attribute
  (as-hiccup [this] [(lower-case-keyword (.getKey this)) (.getValue this)])
  Attributes
  (as-hiccup [this] (into {} (map as-hiccup this)))
  Comment
  (as-hiccup [this] (str "<!--" (.getData this) "-->"))
  DataNode
  (as-hiccup [this] (str this))
  Document
  (as-hiccup [this] (map as-hiccup (.childNodes this)))
  DocumentType
  (as-hiccup [this] (str this))
  Element
  (as-hiccup [this] (into [] (concat [(lower-case-keyword (.tagName this))
                                      (as-hiccup (.attributes this))]
                                     (map as-hiccup (.childNodes this)))))
  TextNode
  (as-hiccup [this] (.getWholeText this))
  XmlDeclaration
  (as-hiccup [this] (str this)))

#+cljs
(defn node-type
  [type]
  (aget js/Node (str type "_NODE")))

#+cljs (def Attribute (node-type "ATTRIBUTE"))
#+cljs (def Comment (node-type "COMMENT"))
#+cljs (def Document (node-type "DOCUMENT"))
#+cljs (def DocumentType (node-type "DOCUMENT_TYPE"))
#+cljs (def Element (node-type "ELEMENT"))
#+cljs (def Text (node-type "TEXT"))

#+cljs
(extend-type js/NodeList
  ISeqable
  (-seq [array] (array-seq array 0)))

#+cljs
(extend-type js/NamedNodeMap
  ISeqable
  (-seq [array] (array-seq array 0)))

#+cljs
(extend-protocol HiccupRepresentable
  object
  (as-hiccup [this] (condp = (aget this "nodeType")
                      Attribute [(lower-case-keyword (aget this "name")) (aget this "nodeValue")]
                      Comment (str "<!--" (aget this "data") "-->")
                      Document (map as-hiccup (vec (aget this "childNodes")))
                      DocumentType (str "<!DOCTYPE " (aget this "name") ">")
                      Element (into [] (concat [(lower-case-keyword (aget this "tagName"))
                                                (into {} (map as-hiccup (aget this "attributes")))]
                                               (map as-hiccup (aget this "childNodes"))))
                      Text (aget this "wholeText"))))

#+clj
(extend-protocol HickoryRepresentable
  Attribute
  (as-hickory [this] [(lower-case-keyword (.getKey this)) (.getValue this)])
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
                      :tag (lower-case-keyword (.tagName this))
                      :content (not-empty
                                (into [] (map as-hickory
                                              (.childNodes this))))})
  TextNode
  (as-hickory [this] (.getWholeText this)))

#+cljs
(extend-protocol HickoryRepresentable
  object
  (as-hickory [this] (condp = (aget this "nodeType")
                       Attribute [(lower-case-keyword (aget this "name")) (aget this "nodeValue")]
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
                                :tag (lower-case-keyword (aget this "tagName"))
                                :content (not-empty
                                           (into [] (map as-hickory
                                                         (aget this "childNodes"))))}
                       Text (aget this "wholeText"))))

#+cljs
(defn extract-doctype
  [s]
  ;;Starting HTML5 doctype definition can be uppercase
  (when-let [doctype (second (or (re-find #"<!DOCTYPE ([^>]*)>" s)
                                 (re-find #"<!doctype ([^>]*)>" s)))]
    (re-find #"([^\s]*)(\s+PUBLIC\s+[\"]?([^\"]*)[\"]?\s+[\"]?([^\"]*)[\"]?)?" doctype)))

#+cljs
(defn remove-el
  [el]
  (.removeChild (aget el "parentNode") el))

#+cljs
(defn parse-dom
  "Parse an HTML document (or fragment) as a DOM using document.implementation.createHTMLDocument and document.write."
  [s]
  ;;See http://www.w3.org/TR/domcore/#dom-domimplementation-createhtmldocument for more details.
  (let [doc (.createHTMLDocument js/document.implementation "");;empty title to be compatible with gecko pre 23 (https://developer.mozilla.org/en-US/docs/Web/API/DOMImplementation.createHTMLDocument)
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
  #+clj (Jsoup/parse s)
  #+cljs (parse-dom s))

(defn parse-fragment
  "Parse an HTML fragment (some group of tags that might be at home somewhere
   in the tag hierarchy under <body>) into a list of DOM elements that can
   each be passed as input to as-hiccup or as-hickory."
  [s]
  #+clj (into [] (Parser/parseFragment s (Element. (Tag/valueOf "body") "") ""))
  #+cljs (aget (parse-dom s) "body" "childNodes"))

(def ^{:private true} void-element
  #{:area :base :br :col :command :embed :hr :img :input :keygen :link :meta
    :param :source :track :wbr})

(def ^{:private true} unescapable-content #{:script :style})

(defn html-escape
  "HTML-escapes the given string."
  [^String s]
  ;; This method is "Java in Clojure" for serious speedups.
  (let [slength (long (count s))
        sb #+clj (StringBuilder. slength) #+cljs (goog.string.StringBuffer.)]
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

(defn- render-attribute
  "Given a map entry m, representing the attribute name and value, returns a
   string representing that key/value pair as it would be rendered into HTML."
  [m]
  (str " " (name (key m)) "=\"" (html-escape (val m)) "\""))

(defn hickory-to-html
  "Given a hickory HTML DOM map structure (as returned by as-hickory), returns a
   string containing HTML it represents.

   Note that it will NOT in general be the case that

     (= my-html-src (hickory-to-html (as-hickory (parse my-html-src))))

   as we do not keep any letter case or whitespace information, any
   \"tag-soupy\" elements, attribute quote characters used, etc."
  [dom]
  (if (string? dom)
    (html-escape dom)
    (try
      (case (:type dom)
        :document
        (apply str (map hickory-to-html (:content dom)))
        :document-type
        (str "<!DOCTYPE " (get-in dom [:attrs :name])
             (when-let [publicid (not-empty (get-in dom [:attrs :publicid]))]
               (str " PUBLIC \"" publicid "\""))
             (when-let [systemid (not-empty (get-in dom [:attrs :systemid]))]
               (str " \"" systemid "\""))
             ">")
        :element
        (cond
         (void-element (:tag dom))
         (str "<" (name (:tag dom))
              (apply str (map render-attribute (:attrs dom)))
              ">")
         (unescapable-content (:tag dom))
         (str "<" (name (:tag dom))
              (apply str (map render-attribute (:attrs dom)))
              ">"
              (apply str (:content dom)) ;; Won't get html-escaped.
              "</" (name (:tag dom)) ">")
         :else
         (str "<" (name (:tag dom))
              (apply str (map render-attribute (:attrs dom)))
              ">"
              (apply str (map hickory-to-html (:content dom)))
              "</" (name (:tag dom)) ">"))
        :comment
        (str "<!--" (apply str (:content dom)) "-->"))
      (catch #+clj IllegalArgumentException #+cljs js/Error e
        (throw
         (if #+clj (.startsWith (.getMessage e) "No matching clause: ") #+cljs (goog.string.startsWith (aget e "message") "No matching clause: ")
           (ex-info (str "Not a valid node: " (pr-str dom)) {:dom dom})
           e))))))
