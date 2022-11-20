(defproject org.clj-commons/hickory (or (System/getenv "PROJECT_VERSION") "0.7.1")
  :description "HTML as Data"
  :url "https://github.com/clj-commons/hickory"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :deploy-repositories [["clojars" {:url "https://repo.clojars.org"
                                    :username :env/clojars_username
                                    :password :env/clojars_password
                                    :sign-releases true}]]

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/clojurescript "1.9.293" :scope "provided"]
                 [org.jsoup/jsoup "1.15.2"]
                 [quoin "0.1.2" :exclusions [org.clojure/clojure]]
                 [viebel/codox-klipse-theme "0.0.1" :scope "provided"]]

  :plugins [[lein-codox "0.10.0"]]

  :source-paths ["src/clj" "src/cljc" "src/cljs"]

  :profiles
  {:dev  {:source-paths ["src/clj" "src/cljc"]}
   :test {:source-paths ["src/cljs" "src/cljc" "test/cljc" "test/cljs"]}}

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
