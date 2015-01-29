(ns optimus.prime)

(defn wrap [app get-assets optimize strategy & [options]]
  (strategy app get-assets optimize (or options {})))
