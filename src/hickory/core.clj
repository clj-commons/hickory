(ns hickory.core
  (:require [clojure.string :as string]
            [clojure.zip :as zip]
            [quoin.text :as qt])
  (:import [org.jsoup Jsoup]
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
  (as-hiccup-impl [this option-map]))

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
  (as-hickory-impl [this option-map]))


(extend-protocol HiccupRepresentable
  Attribute
  (as-hiccup-impl [this option-map] [(lower-case-keyword (.getKey this))
                                     (.getValue this)])
  Attributes
  (as-hiccup-impl [this option-map] (into {} (map #(as-hiccup-impl % option-map)
                                                  this)))
  Comment
  (as-hiccup-impl [this option-map] (str "<!--" (.getData this) "-->"))
  DataNode
  (as-hiccup-impl [this option-map] (str this))
  Document
  (as-hiccup-impl [this option-map] (map #(as-hiccup-impl % option-map)
                                         (.childNodes this)))
  DocumentType
  (as-hiccup-impl [this option-map] (str this))
  Element
  (as-hiccup-impl [this option-map]
    (into [] (concat [(lower-case-keyword (.tagName this))
                      (as-hiccup-impl (.attributes this) option-map)]
                     (map #(as-hiccup-impl % option-map) (.childNodes this)))))
  TextNode
  (as-hiccup-impl [this option-map] (.getWholeText this))
  XmlDeclaration
  (as-hiccup-impl [this option-map] (str this)))

(extend-protocol HickoryRepresentable
  Attribute
  (as-hickory-impl [this option-map] [(lower-case-keyword (.getKey this))
                                      (.getValue this)])
  Attributes
  (as-hickory-impl [this option-map]
    (not-empty (into {} (map #(as-hickory-impl % option-map)
                             this))))
  Comment
  (as-hickory-impl [this option-map] {:type :comment
                                      :content [(.getData this)]})
  DataNode
  (as-hickory-impl [this option-map] (str this))
  Document
  (as-hickory-impl [this option-map]
    {:type :document
     :content (not-empty
               (into [] (map #(as-hickory-impl % option-map)
                             (.childNodes this))))})
  DocumentType
  (as-hickory-impl [this option-map] {:type :document-type
                                      :attrs (as-hickory-impl (.attributes this)
                                                              option-map)})
  Element
  (as-hickory-impl [this option-map]
    {:type :element
     :attrs (as-hickory-impl (.attributes this) option-map)
     :tag (lower-case-keyword (.tagName this))
     :content (not-empty
               (into [] (map #(as-hickory-impl % option-map)
                             (.childNodes this))))})
  TextNode
  (as-hickory-impl [this option-map] (.getWholeText this)))

(def default-options {:unencoded-text-nodes? true})

(defn as-hiccup
  "Converts the node given into a hiccup-format data structure. The
   node must have an implementation of the HiccupRepresentable
   protocol; nodes created by parse or parse-fragment already do.

   Accepts the following options:
       :unencoded-text-nodes? - If true, text nodes will be unencoded
                                (ie, &amp; will appear in the text node as &).
                                Default: true."
  [node & {:as option-map}]
  (as-hiccup-impl node (merge default-options option-map)))

(defn as-hickory
  "Converts the node given into a hickory-format data structure. The
   node must have an implementation of the HickoryRepresentable protocol;
   nodes created by parse or parse-fragment already do.

   Accepts the following options:
       :unencoded-text-nodes? - If true, text nodes will be unencoded
                                (ie, &amp; will appear in the text node as &).
                                Default: true."
  ([node & {:as option-map}]
     (as-hickory-impl node (merge default-options option-map))))

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

(def ^{:private true} void-element
  #{:area :base :br :col :command :embed :hr :img :input :keygen :link :meta
    :param :source :track :wbr})

(defn hickory-to-html
  "Given a hickory HTML DOM map structure (as returned by as-hickory), returns a
   string containing HTML it represents.

   Note that it will NOT in general be the case that

     (= my-html-src (hickory-to-html (as-hickory (parse my-html-src))))

   as we do not keep any letter case or whitespace information, any
   \"tag-soupy\" elements, attribute quote characters used, etc."
  [dom]
  (if (string? dom)
    (qt/html-escape dom)
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
      (if (void-element (:tag dom))
        (str "<" (name (:tag dom))
             (apply str (map #(str " " (name (key %)) "=\"" (val %) "\"")
                             (:attrs dom)))
             ">")
        (str "<" (name (:tag dom))
             (apply str (map #(str " " (name (key %)) "=\"" (val %) "\"")
                             (:attrs dom)))
             ">"
             (apply str (map hickory-to-html (:content dom)))
             "</" (name (:tag dom)) ">"))
      :comment
      (str "<!--" (apply str (:content dom)) "-->"))))