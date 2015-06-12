(ns pedestal-interceptors.service
  (:require [io.pedestal.http :as bootstrap]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [ring.util.response :as ring-resp :refer [response content-type]]
            [io.pedestal.interceptor.helpers :refer [handler defhandler definterceptor]]
            [io.pedestal.interceptor :refer [interceptor]]
            [clojure.pprint :refer [pprint]]))

(def foo-handler
 (handler
  (fn [req]
    (-> (response "<p>This is a test message</p>")
        (content-type "text/html")))))

(defn debug-wrap [handler]
  (assoc handler :wrapper-fn
    (fn [c] (update-in c [:response :body] #(str % "<pre>" (with-out-str (pprint c)) "</pre>")))))

(defn alert-wrap [handler]
  (assoc handler :wrapper-fn
    (fn [c] (update-in c [:response :body] #(str % "<script>alert('Wrapped!');</script>")))))

(def wrapper-interceptor
  (interceptor
   {:name :wrapper-interceptor
    :enter (fn [context]
             (let [wrapper (->> context :io.pedestal.impl.interceptor/queue (some :wrapper-fn))]
               (assoc context :wrapper-fn wrapper)))
    :leave (fn [context]
             (if-let [wrapper (:wrapper-fn context)]
               (wrapper context)))}))

(defroutes routes
  [[["/foo" {:get [:foo-route  (debug-wrap foo-handler) ]}
     ^:interceptors [wrapper-interceptor]]]])

(def service {:env :prod
              ;; You can bring your own non-default interceptors. Make
              ;; sure you include routing and set it up right for
              ;; dev-mode. If you do, many other keys for configuring
              ;; default interceptors will be ignored.
              ;; ::bootstrap/interceptors []
              ::bootstrap/routes routes

              ;; Uncomment next line to enable CORS support, add
              ;; string(s) specifying scheme, host and port for
              ;; allowed source(s):
              ;;
              ;; "http://localhost:8080"
              ;;
              ;;::bootstrap/allowed-origins ["scheme://host:port"]

              ;; Root for resource interceptor that is available by default.
              ::bootstrap/resource-path "/public"

              ;; Either :jetty, :immutant or :tomcat (see comments in project.clj)
              ::bootstrap/type :jetty
              ;;::bootstrap/host "localhost"
              ::bootstrap/port 8080})

