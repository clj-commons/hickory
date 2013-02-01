# Hickory

Hickory parses HTML into Clojure data structures, so you can analyze,
transform, and output back to HTML. HTML can be parsed into
[hiccup](http://github.com/weavejester/hiccup) vectors, or into a
map-based DOM-like format very similar to that used by clojure.xml.

## Usage

To start, you will want to process your HTML into a parsed
representation. Once the HTML is in this form, it can be converted to
either Hiccup or Hickory format for further processing. There are two
parsing functions, `parse` and `parse-fragment`. Both take a string
containing HTML and return the parser objects representing the
document. (It happens that these parser objects are Jsoup Documents
and Nodes, but I do not consider this to be an aspect worth preserving
if a change in parser should become necessary). 

The first function, `parse` expects an entire HTML document, and
parses using an HTML5 parser, [Jsoup](http://jsoup.org), which will
fix up the HTML as much as it can into a well-formed document. The
second function, `parse-fragment`, expects some smaller fragment of
HTML that does not make up a full document, and thus returns a list of
parsed fragments, each of which must be processed individually into
Hiccup or Hickory format. For example, if `parse-fragment` is given
"`<p><br>`" as input, it has no common parent for them, so it must
simply give you the list of nodes that it parsed.

These parsed objects can be turned into either Hiccup vector trees or
Hickory DOM maps using the functions `as-hiccup` or `as-hickory`.

Here's a usage example. 

```clojure
user=> (use 'hickory.core)
nil
user=> (def parsed-doc (parse "<a href=\"foo\">foo</a>"))
#'user/parsed-doc
user=> (as-hiccup parsed-doc)
([:html {} [:head {}] [:body {} [:a {:href "foo"} "foo"]]])
user=> (as-hickory parsed-doc)
{:type :document, :content [{:type :element, :attrs nil, :tag :html, :content [{:type :element, :attrs nil, :tag :head, :content nil} {:type :element, :attrs nil, :tag :body, :content [{:type :element, :attrs {:href "foo"}, :tag :a, :content ["foo"]}]}]}]}
user=> (def parsed-frag (parse-fragment "<a href=\"foo\">foo</a> <a href=\"bar\">bar</a>"))
#'user/parsed-frag
user=> (as-hiccup parsed-frag)
IllegalArgumentException No implementation of method: :as-hiccup of protocol: #'hickory.core/HiccupRepresentable found for class: clojure.lang.PersistentVector  clojure.core/-cache-protocol-fn (core_deftype.clj:495)

user=> (map as-hiccup parsed-frag)
([:a {:href "foo"} "foo"] " " [:a {:href "bar"} "bar"])
user=> (map as-hickory parsed-frag)
({:type :element, :attrs {:href "foo"}, :tag :a, :content ["foo"]} " " {:type :element, :attrs {:href "bar"}, :tag :a, :content ["bar"]})
```

In the example above, you can see an HTML document that is parsed once
and then converted to both Hiccup and Hickory formats. Similarly, a
fragment is parsed, but it cannot be directly used with `as-hiccup`
(or `as-hickory`), it must have those functions called on each element
in the list instead.

The namespace `hickory.zip` provides
[zippers](http://clojure.github.com/clojure/clojure.zip-api.html) for
both Hiccup and Hickory formatted data, with the functions
`hiccup-zip` and `hickory-zip`. Using zippers, you can easily traverse
the trees in any order you desire, make edits, and get the resulting
tree back. Here is an example of that.

```clojure
user=> (use 'hickory.zip)
nil
user=> (require '[clojure.zip :as zip])
nil
user=> (-> (hiccup-zip (as-hiccup (parse "<a href=foo>bar<br></a>"))) zip/node)
([:html {} [:head {}] [:body {} [:a {:href "foo"} "bar" [:br {}]]]])
user=> (-> (hiccup-zip (as-hiccup (parse "<a href=foo>bar<br></a>"))) zip/next zip/node)
[:html {} [:head {}] [:body {} [:a {:href "foo"} "bar" [:br {}]]]]
user=> (-> (hiccup-zip (as-hiccup (parse "<a href=foo>bar<br></a>"))) zip/next zip/next zip/node)
[:head {}]
user=> (-> (hiccup-zip (as-hiccup (parse "<a href=foo>bar<br></a>"))) 
           zip/next zip/next 
           (zip/replace [:head {:id "a"}]) 
           zip/node)
[:head {:id "a"}]
user=> (-> (hiccup-zip (as-hiccup (parse "<a href=foo>bar<br></a>"))) 
           zip/next zip/next 
           (zip/replace [:head {:id "a"}]) 
           zip/root)
([:html {} [:head {:id "a"}] [:body {} [:a {:href "foo"} "bar" [:br {}]]]])
user=> (-> (hickory-zip (as-hickory (parse "<a href=foo>bar<br></a>"))) 
           zip/next zip/next 
           (zip/replace {:type :element :tag :head :attrs {:id "a"} :content nil}) 
           zip/root)
{:type :document, :content [{:type :element, :attrs nil, :tag :html, :content [{:content nil, :type :element, :attrs {:id "a"}, :tag :head} {:type :element, :attrs nil, :tag :body, :content [{:type :element, :attrs {:href "foo"}, :tag :a, :content ["bar" {:type :element, :attrs nil, :tag :br, :content nil}]}]}]}]}
user=> (hickory-to-html *1)
"<html><head id=\"a\"></head><body><a href=\"foo\">bar<br></a></body></html>"
```

In this example, we can see a basic document being parsed into Hiccup
form. Then, using zippers, the HEAD element is navigated to, and then
replaced with one that has an id of "a". The final tree, including the
modification, is also shown using `zip/root`. Then the same
modification is made using Hickory forms and zippers. Finally, the
modified Hickory version is printed back to HTML using the
`hickory-to-html` function.

## Hickory format

Why two formats? It's very easy to see in the example above, Hiccup is
very convenient to use for writing HTML. It has a compact syntax, with
CSS-like shortcuts for specifying classes and ids. It also allows
parts of the vector to be skipped if they are not important.

It's a little bit harder to process data in Hiccup format. First of
all, each form has to be checked for the presence of the attribute
map, and the traversal adjusted accordingly. Raw Hiccup vectors might
also have information about class and id in one of two different
places. Finally, not every piece of an HTML document can be expressed
in Hiccup without resorting to writing HTML in strings. For example,
if you want to put a doctype or comment on your document, it has to be
done as a string in your Hiccup form containing "`<!DOCTYPE html>`" or
"`<!--stuff-->`".

The Hickory format is another data format intended to allow a
roundtrip from HTML as text, into a data structure that is easy to
process and modify, and back into equivalent (but not identical, in
general) HTML. Because it can express all parts of an HTML document in
a parsed form, it is easier to search and modify the structure of the
document.

A Hickory node is either a map or a string. If it is a map, it will
have some subset of the following four keys, depending on the `:type`:

- `:type`    - This will be one of `:comment`, `:document`, `:document-type`, `:element`
- `:tag`     - A node's tag (for example, `:img`). This will only be present for nodes of type `:element`.
- `:attrs`   - A node's attributes, as a map of keywords to values (for example, {:href "/a"}). This will only be present for nodes of type `:element`.
- `:content` - A node's child nodes, in a vector. Only `:comment`, `:document`, and `:element` nodes have children.

Text and CDATA nodes are represented as strings.

This is almost the exact same structure used by
[clojure.xml](http://clojure.github.com/clojure/clojure.xml-api.html),
the only difference being the addition of the `:type` field. Having
this field allows us to process nodes that clojure.xml leaves out of
the parsed data, like doctype and comments.

## Obtaining

To get hickory, add

```clojure
[hickory "0.3.0"]
```

to your project.clj, or an equivalent entry for your Maven-compatible build tool.

## Changes

- Released version 0.3.0. Provides a more helpful error message when hickory-to-html has an error. Now requires Clojure 1.4.

- Released version 0.2.3. Fixes a bug where hickory-to-html was not html-escaping the values of tag attributes.

- Released version 0.2.2. Fixes a bug where hickory-to-html was improperly html-escaping the contents of script/style tags.

- Released version 0.2.1. This version fixes bugs:
    * hickory-to-html now properly escapes text nodes
    * text nodes will now preserve whitespace correctly

- Released version 0.2.0. This version adds a second parsed data
  format, explained above. To support this, the API for `parse` and
  `parse-fragment` has been changed to allow their return values to be
  passed to functions `as-hiccup` or `as-hickory` to determine the
  final format. Also added are zippers for both Hiccup and Hickory
  formats.

## License

Copyright © 2012 David Santiago

Distributed under the Eclipse Public License, the same as Clojure.
