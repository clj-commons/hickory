# Hickory

Hickory parses HTML into Clojure data structures, so you can analyze,
transform, and output back to HTML. HTML can be parsed into
[hiccup](http://github.com/weavejester/hiccup) vectors, or into a
map-based DOM-like format very similar to that used by clojure.xml. It
can be used from both Clojure and Clojurescript.

There is [API documentation](http://davidsantiago.github.com/hickory) available.

## Usage

### Parsing

To start, you will want to process your HTML into a parsed
representation. Once the HTML is in this form, it can be converted to
either Hiccup or Hickory format for further processing. There are two
parsing functions, `parse` and `parse-fragment`. Both take a string
containing HTML and return the parser objects representing the
document. (It happens that these parser objects are Jsoup Documents
and Nodes, but I do not consider this to be an aspect worth preserving
if a change in parser should become necessary).

The first function, `parse` expects an entire HTML document, and
parses it using an HTML5 parser ([Jsoup](http://jsoup.org) on Clojure and
the browser's DOM parser in Clojurescript), which will
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

### Selectors

Hickory also comes with a set of CSS-style selectors that operate on
hickory-format data in the `hickory.select` namespace. These selectors
do not exactly mirror the selectors in CSS, and are often more
powerful. There is no version of these selectors for hiccup-format
data, at this point.

A selector is simply a function that takes a zipper loc from a hickory
html tree data structure as its only argument. The selector will
return its argument if the selector applies to it, and nil
otherwise. Writing useful selectors can often be involved, so most of
the `hickory.select` package is actually made up of selector
combinators; functions that return useful selector functions by
specializing them to the data given as arguments, or by combining
together multiple selectors. For example, if we wanted to figure out
the dates of the next Formula 1 race weekend, we could do something
like this:

```clojure
user=> (use 'hickory.core)
nil
user=> (require '[hickory.select :as s])
nil
user=> (require '[clj-http.client :as client])
nil
user=> (require '[clojure.string :as string])
nil
user=> (def site-htree (-> (client/get "http://formula1.com/default.html") :body parse as-hickory))
#'user/site-htree
user=> (-> (s/select (s/child (s/class "subCalender") ; sic
                              (s/tag :div)
                              (s/id :raceDates)
                              s/first-child
                              (s/tag :b))
                     site-htree)
           first :content first string/trim)
"10, 11, 12 May 2013"
```

In this example, we get the contents of the homepage and use `select`
to give us any nodes that satisfy the criteria laid out by the
selectors. The selector in this example is overly precise in order to
illustrate more selectors than we need; we could have gotten by just
selecting the contents of the P and then B tags inside the element
with id "raceDates".

Using the selectors allows you to search large HTML documents for nodes of interest with a relatively small amount of code. There are many selectors available in the [`hickory.select`](http://davidsantiago.github.io/hickory/hickory.select.html) namespace, including:

- `node-type`: Give this function a keyword or string that names the contents of the `:type` field in a hickory node, and it gives you a selector that will select nodes of that type. Example: `(node-type :comment)`
- `tag`: Give this function a keyword or string that names the contents of the `:tag` field in a hickory node, and it gives you a selector that will select nodes with that tag. Example: `(tag :div)`
- `attr`: Give this function a keyword or string that names an attribute in the `:attrs` map of a hickory node, and it gives you a selector that will select nodes whose `:attrs` map contains that key. Give a single-argument function as an additional argument, and the resulting selector function will additionally require the value of that key to be such that the function given as the last argument returns true. Example: `(attr :id #(.startsWith % "foo"))`
- `id`: Give this function a keyword or string that names the `:id` attribute in the `:attrs` map and it will return a selector function that selects nodes that have that id (this comparison is case-insensitive). Example: `(id :raceDates)`
- `class`: Give this function a keyword or string that names a class that the node should have in the `:class` attribute in the `:attrs` map, and it will return a function that selects nodes that have the given class somewhere in their class string. Example: `(class :foo)`
- `any`: This selector takes no arguments, do not invoke it; returns any node that is an element, similarly to CSS's '*' selector.
- `element`: This selector is equivalent to the `any` selector; this alternate name can make it clearer when the intention is to exclude non-element nodes from consideration.
- `root`: This selector takes no arguments and should not be invoked; simply returns the root node (the HTML element).
- `n-moves-until`: This selector returns a selector function that selects its argument if that argument is some distance from a boundary. The first two arguments, `n` and `c` define the counting: it only selects nodes whose distance can be written in the form `nk+c` for some natural number `k`. The distance and boundary are defined by the number of times the zipper-movement function in the third argument is applied before the boundary function in the last argument is true. See doc string for details.
- `nth-of-type`: This selector returns a selector function that selects its argument if that argument is the `(nk+c)`'th child of the given tag type of some parent node for some natural `k`. Optionally, instead of the `n` and `c` arguments, the keywords `:odd` and `:even` can be given.
- `nth-last-of-type`: Just like `nth-of-type` but counts backwards from the last sibling.
- `nth-child`: This selector returns a selector function that selects its argument if that argument is the `(nk+c)`'th child of its parent node for some natural `k`. Instead of the `n` and `c` arguments, the keywords `:odd` and `:even` can be given.
- `nth-last-child`: Just like `nth-last-child` but counts backwards from the last sibling.
- `first-child`: Takes no arguments, do not invoke it; equivalent to `(nth-child 1)`.
- `last-child`: Takes no arguments, do not invoke it; equivalent to `(nth-last-child 1)`.

There are also selector combinators, which take as argument some number of other selectors, and return a new selector that combines them into one larger selector. An example of this is the `child` selector in the example above. Here's a list of some selector combinators in the package (see the [API Documentation](http://davidsantiago.github.com/hickory) for the full list):

- `and`: Takes any number of selectors, and returns a selector that only selects nodes for which all of the argument selectors are true.
- `or`: Takes any number of selectors, and retrurns a selector that only selects nodes for which at least one of the argument selectors are true.
- `not`: Takes a single selector as argument and returns a selector that only selects nodes that its argument selector does not.
- `el-not`: Takes a single selector as argument and returns a selector that only selects element nodes that its argument selector does not.
- `child`: Takes any number of selectors as arguments and returns a selector that returns true when the zipper location given as the argument is at the end of a chain of direct child relationships specified by the selectors given as arguments.
- `descendant`: Takes any number of selectors as arguments and returns a selector that returns true when the zipper location given as the argument is at the end of a chain of descendant relationships specified by the selectors given as arguments.

We can illustrate the selector combinators by continuing the Formula 1 example above. We suspect, to our dismay, that Sebastian Vettel is leading the championship for the fourth year in a row.

```clojure
user=> (-> (s/select (s/descendant (s/class "subModule")
                                   (s/class "standings")
                                   (s/and (s/tag :tr)
                                          s/first-child)
                                   (s/and (s/tag :td)
                                          (s/nth-child 2))
                                   (s/tag :a))
                     site-htree)
           first :content first string/trim)
"Sebastian Vettel"           
```

Our fears are confirmed, Sebastian Vettel is well on his way to a fourth consecutive championship. If you were to inspect the page by hand (as of around May 2013, at least), you would see that unlike the `child` selector we used in the example above, the `descendant` selector allows the argument selectors to skip stages in the tree; we've left out some elements in this descendant relationship. The first table row in the driver standings table is selected with the `and`, `tag` and `first-child` selectors, and then the second `td` element is chosen, which is the element that has the driver's name (the first table element has the driver's standing) inside an `A` element. All of this is dependent on the exact layout of the HTML in the site we are examining, of course, but it should give an idea of how you can combine selectors to reach into a specific node of an HTML document very easily.

Finally, it's worth noting that the `select` function itself returns the hickory zipper nodes it finds. This is most useful for analyzing the contents of nodes. However, sometimes you may wish to examine the area around a node once you've found it. For this, you can use the `select-locs` function, which returns a sequence of hickory zipper locs, instead of the nodes themselves. This will allow you to navigate around the document tree using the zipper functions in `clojure.zip`. If you wish to go further and actually modify the document tree using zipper functions, you should not use `select-locs`. The problem is that it returns a bunch of zipper locs, but once you modify one, the others are out of date and do not see the changes (just as with any other persistent data structure in Clojure). Thus, their presence was useless and possibly confusing. Instead, you should use the `select-next-loc` function to walk through the document tree manually, moving through the locs that satisfy the selector function one by one, which will allow you to make modifications as you go. As with modifying any data structure as you traverse it, you must still be careful that your code does not add the thing it is selecting for, or it could get caught in an infinite loop. Finally, for more specialized selection needs, it should be possible to write custom selection functions that use the selectors and zipper functions without too much work. The functions discussed in this paragraph are very short and simple, you can use them as a guide.

The doc strings for the functions in the [`hickory.select`](http://davidsantiago.github.io/hickory/hickory.select.html) namespace provide more details on most of these functions.

For more details, see the [API Documentation](http://davidsantiago.github.com/hickory).

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
[hickory "0.7.1"]
```

to your project.clj, or an equivalent entry for your Maven-compatible build tool.

## ClojureScript support

Hickory expects a DOM implementation and thus won't work out of the box on node. On browsers it works for IE9+ (you can find a workaround for IE9 [here](http://stackoverflow.com/questions/9250545/javascript-domparser-access-innerhtml-and-other-properties)).

## Changes

- Version 0.7.1. Thanks to [Matt Grimm](https://github.com/tkocmathla) for adding the up-pred zipper function.

- Version 0.7.0. Thanks to [Ricardo J. Méndez](https://github.com/ricardojmendez) for the following updates.
    * Removed dependency on cljx, since it was deprecated in June 2015.
    * Converted all files and conditionals to cljc.
    * Moved tests to cljs.test with doo, since cemerick.test was deprecated over a year ago.
    * Updated Clojure and ClojureScript dependencies to avoid conflicts.
    * Updated JSoup to 1.9.2, which should bring improved parsing performance.

- Released version 0.6.0.
    * Updated JSoup to version 1.8.3. This version of JSoup contains bug fixes, but slightly changes the way it
    handles HTML: some parses and output might have different case than before. HTML is still case-insensitive,
    of course, but Hickory minor version has been increased just in case. API and semantics are otherwise unchanged.

- Released version 0.5.4.
    * Fixed project dependencies so ClojureScript is moved to a dev-dependency.

- Released version 0.5.3.
    * Minor bug fix to accommodate ClojureScript's new type hinting support.

- Released version 0.5.2.
    * Updates the Clojurescript version to use the latest version of Clojurescript (0.0-1934).

- Released version 0.5.1.
    * Added `has-child` and `has-descendant` selectors. Be careful with `has-descendant`, as it must do a full subtree search on each node, which is not fast.

- Released version 0.5.0.
    * Now works in Clojurescript as well, huge thanks to [Julien Eluard](https://github.com/jeluard) for doing the heavy lifting on this.
    * Reorganized parts of the API into more granular namespaces for better organization.
    * Added functions to convert between Hiccup and Hickory format; note that this conversion is not always exact or roundtripable, and can cause a full HTML reparse.
    * Added new selector, `element-child`, which selects element nodes that are the child of another element node.
    * Numerous bug fixes and improvements.

- Released version 0.4.1, which adds a number of new selectors and selector combinators, including `find-in-text`, `precede-adjacent`, `follow-adjacent`, `precede` and `follow`.

- Released version 0.4.0. Adds the `hickory.select` namespace with many helpful functions for searching through hickory-format HTML documents for specific nodes.

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
