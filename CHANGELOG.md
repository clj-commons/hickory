# Changelog

[Hickory](https://github.com/clj-commons/hickory): parse HTML into Clojure data structures.

## Unreleased

- [#98](https://github.com/clj-commons/hickory/issues/98): `as-hiccup` converts the element after an empty `script` or `style` element ([@jacobemcken](https://github.com/jacobemcken))
- [#94](https://github.com/clj-commons/hickory/pull/94): `parse` accepts a `String`, `File` or `Path` without reflection and throws on other input ([@erdos](https://github.com/erdos))
- Bump jsoup to 1.22.2

## 0.7.7 (2025-03-13)

- [#92](https://github.com/clj-commons/hickory/pull/92): Suppress the reflection warning in `parse`

## 0.7.6 (2025-03-11)

- Bump jsoup to 1.19.1

## 0.7.5 (2024-10-16)

- Add `parent` and `ancestor` selectors
- Fix `find-in-text` inside a child selector

## 0.7.4 (2023-12-22)

- Remove the quoin dependency
- Split class names on any whitespace
- Fix ClojureScript advanced compilation

## 0.7.3 (2022-11-20)

- Upgrade ClojureScript

## 0.7.2 (2022-11-20)

- Move to clj-commons: `org.clj-commons/hickory`
- Parse deeply nested HTML without a stack overflow
- Support Node.js in ClojureScript
- Bump jsoup to 1.15.2

## 0.7.1 (2017-03-30)

- Add the `up-pred` zipper function ([@tkocmathla](https://github.com/tkocmathla))

## 0.7.0 (2016-10-30)

Thanks to [Ricardo J. Méndez](https://github.com/ricardojmendez) for the following updates.

- Removed dependency on cljx, since it was deprecated in June 2015.
- Converted all files and conditionals to cljc.
- Moved tests to cljs.test with doo, since cemerick.test was deprecated over a year ago.
- Updated Clojure and ClojureScript dependencies to avoid conflicts.
- Updated JSoup to 1.9.2, which should bring improved parsing performance.

## 0.6.0 (2016-01-04)

- Updated JSoup to version 1.8.3. This version of JSoup contains bug fixes, but slightly changes the way it
  handles HTML: some parses and output might have different case than before. HTML is still case-insensitive,
  of course, but Hickory minor version has been increased just in case. API and semantics are otherwise unchanged.

## 0.5.4

- Fixed project dependencies so ClojureScript is moved to a dev-dependency.

## 0.5.3

- Minor bug fix to accommodate ClojureScript's new type hinting support.

## 0.5.2

- Updates the Clojurescript version to use the latest version of Clojurescript (0.0-1934).

## 0.5.1

- Added `has-child` and `has-descendant` selectors. Be careful with `has-descendant`, as it must do a full subtree search on each node, which is not fast.

## 0.5.0

- Now works in Clojurescript as well, huge thanks to [Julien Eluard](https://github.com/jeluard) for doing the heavy lifting on this.
- Reorganized parts of the API into more granular namespaces for better organization.
- Added functions to convert between Hiccup and Hickory format; note that this conversion is not always exact or roundtripable, and can cause a full HTML reparse.
- Added new selector, `element-child`, which selects element nodes that are the child of another element node.
- Numerous bug fixes and improvements.

## 0.4.1

- Adds a number of new selectors and selector combinators, including `find-in-text`, `precede-adjacent`, `follow-adjacent`, `precede` and `follow`.

## 0.4.0

- Adds the `hickory.select` namespace with many helpful functions for searching through hickory-format HTML documents for specific nodes.

## 0.3.0

- Provides a more helpful error message when hickory-to-html has an error. Now requires Clojure 1.4.

## 0.2.3

- Fixes a bug where hickory-to-html was not html-escaping the values of tag attributes.

## 0.2.2

- Fixes a bug where hickory-to-html was improperly html-escaping the contents of script/style tags.

## 0.2.1

- hickory-to-html now properly escapes text nodes
- text nodes will now preserve whitespace correctly

## 0.2.0

- Adds a second parsed data format, explained in the README. To support this, the API for `parse` and
  `parse-fragment` has been changed to allow their return values to be
  passed to functions `as-hiccup` or `as-hickory` to determine the
  final format. Also added are zippers for both Hiccup and Hickory
  formats.
