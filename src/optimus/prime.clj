(ns optimus.prime)

(defn wrap [app get-assets optimize strategy & {:as options}]
  (strategy app get-assets optimize (or options {})))
