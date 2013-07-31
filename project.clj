(defproject hickory "0.5.0"
  :description "HTML as Data"
  :url "http://github.com/davidsantiago/hickory"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src"  "target/generated-src"]
  :test-paths ["target/generated-test"]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1847"]
                 [quoin "0.1.0"]
                 [org.jsoup/jsoup "1.7.1"]
                 [com.cemerick/clojurescript.test "0.0.4"]]
  :plugins [[codox "0.6.4"]
            [lein-cljsbuild "0.3.2"]
            [com.keminglabs/cljx "0.3.0"]]
  :hooks [cljx.hooks]
  :codox {:sources ["src" "target/generated-src"]
          :output-dir "codox-out"
          :src-dir-uri "http://github.com/davidsantiago/hickory/blob/master"
          :src-linenum-anchor-prefix "L"}

  :cljx {:builds [{:source-paths ["src"]
                   :output-path "target/generated-src"
                   :rules :clj}
                  {:source-paths ["src"]
                   :output-path "target/generated-src"
                   :rules :cljs}
                  {:source-paths ["test"]
                   :output-path "target/generated-test"
                   :rules :clj}
                  {:source-paths ["test"]
                   :output-path "target/generated-test"
                   :rules :cljs}]}
  :cljsbuild {:builds [{:source-paths ["target/generated-src" "target/generated-test"]
                        :compiler {:output-to "target/cljs/testable.js"}
                        :optimizations :whitespace
                        :pretty-print true}]
              :test-commands {"unit-tests" ["phantomjs"  "runners/phantomjs.js" "target/cljs/testable.js"]}})

