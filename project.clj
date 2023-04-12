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
                 [org.clojure/clojurescript "1.11.60" :scope "provided"]
                 [org.jsoup/jsoup "1.15.2"]]

  :source-paths ["src/clj" "src/cljc" "src/cljs"]

  :profiles
  {:dev  {:source-paths ["src/clj" "src/cljc"]}
   :test {:source-paths ["src/cljs" "src/cljc" "test/cljc" "test/cljs"]}})
