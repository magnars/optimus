(ns optimus.prime-test
  (:use optimus.prime
        midje.sweet))

(defn strategy [app get-assets options]
  (fn [request]
    (app (-> request
             (assoc :assets (get-assets))
             (assoc :options options)))))

(defn get-my-assets []
  :my-assets)

(defn my-app [request]
  (assoc request :my-app :was-here))

(fact
 "Wrap is truly a wrapper. All it does is call the chosen strategy
  with the app and get-assets function. It's just there for sugary
  middleware chaining and strategy choice."

 (def app (-> my-app
              (wrap get-my-assets strategy)))

 (app {:uri "/"}) => {:uri "/"
                      :assets :my-assets
                      :my-app :was-here
                      :options {}})

(fact
 "You can also pass in options, which will be passed to the
  strategy."

 (def app (-> my-app
              (wrap get-my-assets strategy
                    :option1 "a"
                    :option2 "b")))

 (:options (app {:uri "/"})) => {:option1 "a"
                                 :option2 "b"})
