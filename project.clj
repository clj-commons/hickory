(defproject hickory "0.4.1"
  :description "HTML as Data"
  :url "http://github.com/davidsantiago/hickory"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [quoin "0.1.0"]
                 [org.jsoup/jsoup "1.7.1"]]
  :plugins [[codox "0.6.4"]]

  :codox {:output-dir "codox-out"
          :src-dir-uri "http://github.com/davidsantiago/hickory/blob/master"
          :src-linenum-anchor-prefix "L"})

