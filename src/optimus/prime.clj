(ns optimus.prime)

(defn wrap [app get-assets strategy & {:as options}]
  (strategy app get-assets (or options {})))
