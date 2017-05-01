(ns server.handler
  (:gen-class)
  (:require
    [clojure.tools.cli :refer [parse-opts]]
    [compojure.core :refer [defroutes GET POST]]
    [compojure.handler :as handler]
    [compojure.route :as route]
    [org.httpkit.server :refer [run-server]]
    [ring.middleware.reload :refer [wrap-reload]]
    [server.tsne :refer [tsne]]))


(defroutes app-routes
  (route/resources "/")
  (GET "/" [] tsne))


(def app
  (-> #'app-routes
      wrap-reload
      handler/site))

(def cli-options
  [["-p" "--port PORT" "Port number"
    :default 3000
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]])


(defn -main [& args]
  (let [{:keys [options]} (parse-opts args cli-options)
        {:keys [port summary]} options]
    (if (and port)
      (do
        (run-server app {:port (:port options)})
        (println "Server started"))
      (println summary))))

