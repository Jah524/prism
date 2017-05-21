(defproject jah524/prism "0.5.1"
  :description "A handy neural network library for natural language processing written in pure Clojure"
  :url "https://github.com/Jah524/prism"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :jvm-opts ["-Xmx12G" "-Xms4096m" "-server"]
  :test-selectors {:default (complement :native)
                   :native  :native
                   :all     (constantly true)}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.521"]
                 [net.mikera/vectorz-clj "0.47.0"]
                 [net.mikera/core.matrix "0.60.3"]
                 [uncomplicate/neanderthal "0.9.0"]
                 [clj-time "0.13.0"]
                 [com.taoensso/nippy "2.13.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/core.async "0.3.442"]
                 [incanter "1.5.7"]
                 [com.googlecode.efficient-java-matrix-library/core "0.26"]
                 [thinktopic/think.tsne "0.1.1"]
                 [http-kit "2.1.16"]
                 [compojure "1.5.1"]
                 [ring "1.5.0"]
                 [org.clojure/tools.cli "0.3.5"]
                 [cljs-ajax "0.5.9"]
                 ]
  :plugins [[lein-cljsbuild "1.1.1"]]
  :cljsbuild {:builds [{:id "tsne"
                        :source-paths ["src/server/cljs/tsne"]
                        :compiler {:output-to  "resources/public/js/tsne.js"
                                   :output-dir "resources/public/js/tsne"
                                   :optimizations :advanced
                                   :externs ["server/externs/jquery-1.9.js"
                                             "server/externs/vue.js"
                                             "server/externs/plotly.js"]}}
                       {:id "word-probability-given-context"
                        :source-paths ["src/server/cljs/word_probability_given_context"]
                        :compiler {:output-to  "resources/public/js/word-probability-given-context.js"
                                   :output-dir "resources/public/js/word-probability-given-context"
                                   :optimizations :advanced
                                   :externs ["server/externs/jquery-1.9.js"
                                             "server/externs/vue.js"
                                             "server/externs/plotly.js"]}}]})
