(defproject org.clj-commons/hickory (or (System/getenv "PROJECT_VERSION") "0.7.1")
  :description "HTML as Data"
  :url "https://github.com/clj-commons/hickory"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :deploy-repositories [["clojars" {:url "https://repo.clojars.org"
                                    :username :env/clojars_username
                                    :password :env/clojars_password
                                    :sign-releases true}]]

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.293" :scope "provided"]
                 [org.jsoup/jsoup "1.14.3"]
                 [quoin "0.1.2" :exclusions [org.clojure/clojure]]
                 [viebel/codox-klipse-theme "0.0.1" :scope "provided"]]

  :hooks [leiningen.cljsbuild]

  :plugins [[lein-codox "0.10.0"]]
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
          :output-path                "codox-out"
          :src-dir-uri               "http://github.com/davidsantiago/hickory/blob/master"
          :metadata {:doc/format :markdown}
          :language :clojurescript
          :themes [:default [:klipse {:klipse/external-libs  "https://raw.githubusercontent.com/davidsantiago/hickory/master/src/cljc,https://raw.githubusercontent.com/davidsantiago/hickory/master/src/cljs"
                                      :klipse/require-statement "(ns my.html
                                                                  (:require [hickory.core :refer [parse as-hiccup as-hickory]]
                                                                            [hickory.render :refer [hickory-to-html hiccup-to-html]]
                                                                            [hickory.convert :refer [hiccup-to-hickory]]))
                                                                "}]]
          :src-linenum-anchor-prefix "L"}

  )
