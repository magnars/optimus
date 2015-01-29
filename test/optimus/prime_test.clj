(ns optimus.prime-test
  (:use optimus.prime
        midje.sweet))

(defn strategy [app get-assets optimize options]
  (fn [request]
    (app (-> request
             (assoc :assets (optimize (get-assets)))
             (assoc :options options)))))

(defn get-my-assets []
  [{:name "my-asset" :size 10}])

(defn optimize [assets]
  (map #(assoc % :size 1) assets))

(defn my-app [request]
  (assoc request :my-app :was-here))

(fact
 "Wrap is truly a wrapper. All it does is call the chosen strategy
  with the app and get-assets function. It's just there for sugary
  middleware chaining and strategy choice."

 (def app (-> my-app
              (wrap get-my-assets optimize strategy)))

 (app {:uri "/"}) => {:uri "/"
                      :assets [{:name "my-asset" :size 1}]
                      :my-app :was-here
                      :options {}})

(fact
 "You can also pass in options, which will be passed to the
  strategy."

 (def app (-> my-app
              (wrap get-my-assets optimize strategy
                    {:option1 "a"
                     :option2 "b"})))

 (:options (app {:uri "/"})) => {:option1 "a"
                                 :option2 "b"})
