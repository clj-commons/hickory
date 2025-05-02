# Table of contents
-  [`hickory.core`](#hickory.core) 
    -  [`Attribute`](#hickory.core/attribute)
    -  [`Comment`](#hickory.core/comment)
    -  [`Document`](#hickory.core/document)
    -  [`DocumentType`](#hickory.core/documenttype)
    -  [`Element`](#hickory.core/element)
    -  [`HiccupRepresentable`](#hickory.core/hiccuprepresentable) - Objects that can be represented as Hiccup nodes implement this protocol in order to make the conversion.
    -  [`HickoryRepresentable`](#hickory.core/hickoryrepresentable) - Objects that can be represented as HTML DOM node maps, similar to clojure.xml, implement this protocol to make the conversion.
    -  [`Text`](#hickory.core/text)
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






## <a name="hickory.core/attribute">`Attribute`</a>



<p><sub><a href="https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L52-L52">Source</a></sub></p>

## <a name="hickory.core/comment">`Comment`</a>



<p><sub><a href="https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L53-L53">Source</a></sub></p>

## <a name="hickory.core/document">`Document`</a>



<p><sub><a href="https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L54-L54">Source</a></sub></p>

## <a name="hickory.core/documenttype">`DocumentType`</a>



<p><sub><a href="https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L55-L55">Source</a></sub></p>

## <a name="hickory.core/element">`Element`</a>



<p><sub><a href="https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L56-L56">Source</a></sub></p>

## <a name="hickory.core/hiccuprepresentable">`HiccupRepresentable`</a>




Objects that can be represented as Hiccup nodes implement this protocol in
   order to make the conversion.
<p><sub><a href="https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L12-L18">Source</a></sub></p>

## <a name="hickory.core/hickoryrepresentable">`HickoryRepresentable`</a>




Objects that can be represented as HTML DOM node maps, similar to
   clojure.xml, implement this protocol to make the conversion.

   Each DOM node will be a map or string (for Text/CDATASections). Nodes that
   are maps have the appropriate subset of the keys

     :type     - [:comment, :document, :document-type, :element]
     :tag      - node's tag, check :type to see if applicable
     :attrs    - node's attributes as a map, check :type to see if applicable
     :content  - node's child nodes, in a vector, check :type to see if
                 applicable
<p><sub><a href="https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L20-L35">Source</a></sub></p>

## <a name="hickory.core/text">`Text`</a>



<p><sub><a href="https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L57-L57">Source</a></sub></p>

## <a name="hickory.core/as-hiccup">`as-hiccup`</a>
``` clojure

(as-hiccup this)
```
Function.

Converts the node given into a hiccup-format data structure. The
     node must have an implementation of the HiccupRepresentable
     protocol; nodes created by parse or parse-fragment already do.
<p><sub><a href="https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L15-L18">Source</a></sub></p>

## <a name="hickory.core/as-hickory">`as-hickory`</a>
``` clojure

(as-hickory this)
```
Function.

Converts the node given into a hickory-format data structure. The
     node must have an implementation of the HickoryRepresentable protocol;
     nodes created by parse or parse-fragment already do.
<p><sub><a href="https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L32-L35">Source</a></sub></p>

## <a name="hickory.core/extract-doctype">`extract-doctype`</a>
``` clojure

(extract-doctype s)
```
Function.
<p><sub><a href="https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L121-L126">Source</a></sub></p>

## <a name="hickory.core/format-doctype">`format-doctype`</a>
``` clojure

(format-doctype dt)
```
Function.
<p><sub><a href="https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L62-L69">Source</a></sub></p>

## <a name="hickory.core/node-type">`node-type`</a>
``` clojure

(node-type type)
```
Function.
<p><sub><a href="https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L37-L50">Source</a></sub></p>

## <a name="hickory.core/parse">`parse`</a>
``` clojure

(parse s)
```
Function.

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


  
<p><sub><a href="https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L151-L168">Source</a></sub></p>

## <a name="hickory.core/parse-dom-with-domparser">`parse-dom-with-domparser`</a>
``` clojure

(parse-dom-with-domparser s)
```
Function.
<p><sub><a href="https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L132-L135">Source</a></sub></p>

## <a name="hickory.core/parse-dom-with-write">`parse-dom-with-write`</a>
``` clojure

(parse-dom-with-write s)
```
Function.

Parse an HTML document (or fragment) as a DOM using document.implementation.createHTMLDocument and document.write.
<p><sub><a href="https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L137-L149">Source</a></sub></p>

## <a name="hickory.core/parse-fragment">`parse-fragment`</a>
``` clojure

(parse-fragment s)
```
Function.

Parse an HTML fragment (some group of tags that might be at home somewhere
   in the tag hierarchy under <body>) into a list of DOM elements that can
   each be passed as input to as-hiccup or as-hickory.
<p><sub><a href="https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L170-L175">Source</a></sub></p>

## <a name="hickory.core/remove-el">`remove-el`</a>
``` clojure

(remove-el el)
```
Function.
<p><sub><a href="https://github.com/clj-commons/hickory/blob/master/src/cljs/hickory/core.cljs#L128-L130">Source</a></sub></p>
