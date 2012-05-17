# Hickory

Hickory parses HTML into [hiccup](http://github.com/weavejester/hiccup) vectors.

## Usage

There are only two functions, `parse` and `parse-fragment`. Both take a string containing HTML and return the sequence of hiccup vectors representing the document. It should be possible to pass this structure back into hiccup and get an equivalent HTML document back out (hiccup can't guarantee the whitespace and formatting will be the same).

The first function, `parse` expects an entire HTML document, and parses using an HTML5 parser, [Jsoup](http://jsoup.org), which will fix up the HTML as much as it can into a well-formed document. The seconf function, `parse-fragment`, expects some smaller fragment of HTML that does not make up a full document. 

Here's an example. 

```clojure
user=> (use 'hickory.core)
nil
user=> (parse "<a href=\"foo\">foo</a>")
[[:html {} [:head {}] [:body {} [:a {:href "foo"} "foo"]]]]
user=> (parse-fragment "<a href=\"foo\">foo</a> <a href=\"bar\">bar</a>")
[[:a {:href "foo"} "foo"] " " [:a {:href "bar"} "bar"]]
```

To get hickory, add

```clojure
[hickory "0.1.0"]
```

to your project.clj, or an equivalent entry for your Maven-compatible build tool.

## License

Copyright © 2012 David Santiago

Distributed under the Eclipse Public License, the same as Clojure.
