(ns optimus.prime-test
  (:use optimus.prime
        midje.sweet))

(fact
 "Wrap is truly a wrapper. All it does is call the chosen strategy
  with the app and get-assets function. It's just there for sugary
  middleware chaining and strategy choice."

 (defn strategy [app get-assets]
   (fn [request]
     (app (assoc request :assets (get-assets)))))

 (defn get-my-assets []
   :my-assets)

 (defn my-app [request]
   (assoc request :my-app :was-here))

 (def app (-> my-app
              (wrap get-my-assets strategy)))

 (app {:uri "/"}) => {:uri "/"
                      :assets :my-assets
                      :my-app :was-here})
