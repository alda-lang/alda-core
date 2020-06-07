(ns alda.dev
  (:require [alda.parser   :as p]
            [alda.server   :as s]
            [alda.util     :as util]
            [alda.worker   :as w]
            [cheshire.core :as json]
            [ezzmq.core    :as zmq]))

(defn get-backend-port
  [frontend-port]
  ;; This is a hack due to with-new-context not returning the last value of its
  ;; body. I need to fix this in ezzmq.
  (let [result (atom nil)]
    (zmq/with-new-context
      (let [socket (zmq/socket :req {:connect (str "tcp://*:" frontend-port)})]
        (as-> "status" x
          (zmq/send-msg socket [(json/generate-string {:command x}) x])
          (zmq/receive-msg socket :stringify true)
          (first x)
          (json/parse-string x true)
          (:body x)
          (re-find #"backend port: (\d+)" x)
          (second x)
          (Integer/parseInt x)
          (reset! result x))))
    @result))

(defn start-server!
  [frontend-port]
  (binding [s/*no-system-exit*     true
            s/*disable-supervisor* true]
    (s/start-server! 0 frontend-port :verbose)))

(defn start-worker!
  [backend-port]
  (binding [w/*no-system-exit* true]
    (w/start-worker! backend-port :verbose)))

(defn -main
  [frontend-port workers]
  (future (start-server! frontend-port))
  (let [backend-port (get-backend-port frontend-port)]
    (dotimes [_ workers]
      (future (start-worker! backend-port)))))

;;; scratchwork

(comment
  (util/set-log-level! :debug)

  (p/parse-input
    "(quant! 50)
     (tempo! 120)

     piano:
        V1: {g a- g}16 f16 (quant! 80) c
        V2: o1 a-8 g c b")

  (p/parse-input
    "(quant! 50)
     (tempo! 120)

     piano:
        V1: g16 f16 (quant! 80) c
        V2: o1 a-8 g c b"))

