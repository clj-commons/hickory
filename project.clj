(defproject hickory "0.7.0-SNAPSHOT"
  :description "HTML as Data"
  :url "http://github.com/davidsantiago/hickory"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}


  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.293"]
                 [org.jsoup/jsoup "1.9.2"]
                 [quoin "0.1.2" :exclusions [org.clojure/clojure]]]

  :hooks [leiningen.cljsbuild]

  :plugins [[codox "0.6.4"]]
  :doo {:build "test"}

  :source-paths ["src/clj" "src/cljc" "src/cljs"]

  :cljsbuild {:builds        {}
              :test-commands {"unit-tests" ["lein" "with-profile" "test" "doo" "phantom" "once"]}}

  :profiles
  {:dev  {:source-paths ["src/clj" "src/cljc"]
          :dependencies [[lein-doo "0.1.7"]]
          :plugins      [[lein-cljsbuild "1.1.4"]
                         [lein-doo "0.1.7" :exclusions [org.clojure/clojure]]]}
   :test {:source-paths ["src/cljs" "src/cljc" "test/cljc" "test/cljs"]
          :plugins      [[lein-doo "0.1.7" :exclusions [org.clojure/clojure]]]
          :cljsbuild    {:builds {:test {:source-paths  ["src/cljs" "src/cljc" "test/cljc" "test/cljs"]
                                         :compiler      {:output-to "target/cljs/testable.js"
                                                         :main      "hickory.doo-runner"}
                                         :optimizations :whitespace
                                         :pretty-print  true}}}
          }}

  :codox {:sources                   ["src" "target/generated-src"]
          :output-dir                "codox-out"
          :src-dir-uri               "http://github.com/davidsantiago/hickory/blob/master"
          :src-linenum-anchor-prefix "L"}

  )
