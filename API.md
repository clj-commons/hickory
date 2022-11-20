# Table of contents
-  [`hickory.core`](#hickory.core) 
    -  [`Attribute`](#hickory.core/Attribute)
    -  [`Comment`](#hickory.core/Comment)
    -  [`Document`](#hickory.core/Document)
    -  [`DocumentType`](#hickory.core/DocumentType)
    -  [`Element`](#hickory.core/Element)
    -  [`HiccupRepresentable`](#hickory.core/HiccupRepresentable) - Objects that can be represented as Hiccup nodes implement this protocol in order to make the conversion.
    -  [`HickoryRepresentable`](#hickory.core/HickoryRepresentable) - Objects that can be represented as HTML DOM node maps, similar to clojure.xml, implement this protocol to make the conversion.
    -  [`Text`](#hickory.core/Text)
    -  [`as-hiccup`](#hickory.core/as-hiccup) - Converts the node given into a hiccup-format data structure.
    -  [`as-hickory`](#hickory.core/as-hickory) - Converts the node given into a hickory-format data structure.
    -  [`extract-doctype`](#hickory.core/extract-doctype)
    -  [`format-doctype`](#hickory.core/format-doctype)
    -  [`node-type`](#hickory.core/node-type)
    -  [`parse`](#hickory.core/parse) - Parse an entire HTML document into a DOM structure that can be used as input to as-hiccup or as-hickory.
    -  [`parse-dom-with-domparser`](#hickory.core/parse-dom-with-domparser)
    -  [`parse-dom-with-write`](#hickory.core/parse-dom-with-write) - Parse an HTML document (or fragment) as a DOM using document.implementation.createHTMLDocument and document.write.
    -  [`parse-fragment`](#hickory.core/parse-fragment) - Parse an HTML fragment (some group of tags that might be at home somewhere in the tag hierarchy under <body>) into a list of DOM elements that can each be passed as input to as-hiccup or as-hickory.
    -  [`remove-el`](#hickory.core/remove-el)

-----
# <a name="hickory.core">hickory.core</a>






## <a name="hickory.core/Attribute">`Attribute`</a> [:page_facing_up:](https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L40-L40)
<a name="hickory.core/Attribute"></a>

## <a name="hickory.core/Comment">`Comment`</a> [:page_facing_up:](https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L41-L41)
<a name="hickory.core/Comment"></a>

## <a name="hickory.core/Document">`Document`</a> [:page_facing_up:](https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L42-L42)
<a name="hickory.core/Document"></a>

## <a name="hickory.core/DocumentType">`DocumentType`</a> [:page_facing_up:](https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L43-L43)
<a name="hickory.core/DocumentType"></a>

## <a name="hickory.core/Element">`Element`</a> [:page_facing_up:](https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L44-L44)
<a name="hickory.core/Element"></a>

## <a name="hickory.core/HiccupRepresentable">`HiccupRepresentable`</a> [:page_facing_up:](https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L11-L17)
<a name="hickory.core/HiccupRepresentable"></a>

Objects that can be represented as Hiccup nodes implement this protocol in
   order to make the conversion.

## <a name="hickory.core/HickoryRepresentable">`HickoryRepresentable`</a> [:page_facing_up:](https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L19-L34)
<a name="hickory.core/HickoryRepresentable"></a>

Objects that can be represented as HTML DOM node maps, similar to
   clojure.xml, implement this protocol to make the conversion.

   Each DOM node will be a map or string (for Text/CDATASections). Nodes that
   are maps have the appropriate subset of the keys

     :type     - [:comment, :document, :document-type, :element]
     :tag      - node's tag, check :type to see if applicable
     :attrs    - node's attributes as a map, check :type to see if applicable
     :content  - node's child nodes, in a vector, check :type to see if
                 applicable

## <a name="hickory.core/Text">`Text`</a> [:page_facing_up:](https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L45-L45)
<a name="hickory.core/Text"></a>

## <a name="hickory.core/as-hiccup">`as-hiccup`</a> [:page_facing_up:](https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L14-L17)
<a name="hickory.core/as-hiccup"></a>
``` clojure

(as-hiccup this)
```


Converts the node given into a hiccup-format data structure. The
     node must have an implementation of the HiccupRepresentable
     protocol; nodes created by parse or parse-fragment already do.

## <a name="hickory.core/as-hickory">`as-hickory`</a> [:page_facing_up:](https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L31-L34)
<a name="hickory.core/as-hickory"></a>
``` clojure

(as-hickory this)
```


Converts the node given into a hickory-format data structure. The
     node must have an implementation of the HickoryRepresentable protocol;
     nodes created by parse or parse-fragment already do.

## <a name="hickory.core/extract-doctype">`extract-doctype`</a> [:page_facing_up:](https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L109-L114)
<a name="hickory.core/extract-doctype"></a>
``` clojure

(extract-doctype s)
```


## <a name="hickory.core/format-doctype">`format-doctype`</a> [:page_facing_up:](https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L50-L57)
<a name="hickory.core/format-doctype"></a>
``` clojure

(format-doctype dt)
```


## <a name="hickory.core/node-type">`node-type`</a> [:page_facing_up:](https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L36-L38)
<a name="hickory.core/node-type"></a>
``` clojure

(node-type type)
```


## <a name="hickory.core/parse">`parse`</a> [:page_facing_up:](https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L139-L156)
<a name="hickory.core/parse"></a>
``` clojure

(parse s)
```


Parse an entire HTML document into a DOM structure that can be
   used as input to as-hiccup or as-hickory.

```klipse
  (-> (parse "<a style=\"visibility:hidden\">foo</a><div style=\"color:green\"><p>Hello</p></div>")
    as-hiccup)
```

```klipse
  (-> (parse "<a style=\"visibility:hidden\">foo</a><div style=\"color:green\"><p>Hello</p></div>")
    as-hickory)
```


  

## <a name="hickory.core/parse-dom-with-domparser">`parse-dom-with-domparser`</a> [:page_facing_up:](https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L120-L123)
<a name="hickory.core/parse-dom-with-domparser"></a>
``` clojure

(parse-dom-with-domparser s)
```


## <a name="hickory.core/parse-dom-with-write">`parse-dom-with-write`</a> [:page_facing_up:](https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L125-L137)
<a name="hickory.core/parse-dom-with-write"></a>
``` clojure

(parse-dom-with-write s)
```


Parse an HTML document (or fragment) as a DOM using document.implementation.createHTMLDocument and document.write.

## <a name="hickory.core/parse-fragment">`parse-fragment`</a> [:page_facing_up:](https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L158-L163)
<a name="hickory.core/parse-fragment"></a>
``` clojure

(parse-fragment s)
```


Parse an HTML fragment (some group of tags that might be at home somewhere
   in the tag hierarchy under <body>) into a list of DOM elements that can
   each be passed as input to as-hiccup or as-hickory.

## <a name="hickory.core/remove-el">`remove-el`</a> [:page_facing_up:](https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L116-L118)
<a name="hickory.core/remove-el"></a>
``` clojure

(remove-el el)
```

