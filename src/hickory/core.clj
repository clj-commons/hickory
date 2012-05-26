(ns hickory.core
  (:require [clojure.string :as string]
            [clojure.zip :as zip])
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
  (as-hiccup [this]))

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
  (as-hickory [this]))


(extend-protocol HiccupRepresentable
  Attribute
  (as-hiccup [this] [(lower-case-keyword (.getKey this))
                     (.getValue this)])
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
  (as-hiccup [this] (.text this))
  XmlDeclaration
  (as-hiccup [this] (str this)))

(extend-protocol HickoryRepresentable
  Attribute
  (as-hickory [this] [(lower-case-keyword (.getKey this))
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
                      :content (not-empty (into [] (map as-hickory
                                                        (.childNodes this))))})
  DocumentType
  (as-hickory [this] {:type :document-type
                      :attrs (as-hickory (.attributes this))})
  Element
  (as-hickory [this] {:type :element
                      :attrs (as-hickory (.attributes this))
                      :tag (lower-case-keyword (.tagName this))
                      :content (not-empty (into [] (map as-hickory
                                                        (.childNodes this))))})
  TextNode
  (as-hickory [this] (.text this)))

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
    dom
    (case (:type dom)
      :document
      (apply str (map hickory-to-html (:content dom)))
      :document-type
      (str "<!DOCTYPE " (get-in dom [:attrs :name])
           (get-in dom [:attrs :publicid]) (get-in dom [:attrs :systemid]) ">")
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