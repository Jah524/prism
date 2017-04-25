(defproject jah524/prism "0.3.1"
  :description "A handy neural network library for natural language processing written in pure Clojure"
  :url "https://github.com/Jah524/prism"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :jvm-opts ["-Xmx15G" "-Xms4096m" "-server"]
  :test-selectors {:default (complement :native)
                   :native :native
                   :all     (constantly true)}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [uncomplicate/neanderthal "0.9.0"]
                 [clj-time "0.13.0"]
                 [com.taoensso/nippy "2.13.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/core.async "0.3.442"]
                 [incanter "1.5.7"]])
