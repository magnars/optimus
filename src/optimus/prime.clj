(ns optimus.prime)

(defn wrap [app get-assets strategy]
  (strategy app get-assets))
