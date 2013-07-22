(ns hickory.convert
  (:require [hickory.hiccup-utils :as hu]
            [quoin.text :as qt]))

(def ^{:private true} void-element
  #{:area :base :br :col :command :embed :hr :img :input :keygen :link :meta
    :param :source :track :wbr})

(def ^{:private true} unescapable-content #{:script :style})

(defn- render-attribute
  "Given a map entry m, representing the attribute name and value, returns a
   string representing that key/value pair as it would be rendered into HTML."
  [m]
  (str " " (name (key m)) "=\"" (qt/html-escape (val m)) "\""))

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
      (catch IllegalArgumentException e
        (throw
         (if (.startsWith (.getMessage e) "No matching clause: ")
           (ex-info (str "Not a valid node: " (pr-str dom)) {:dom dom})
           e))))))
