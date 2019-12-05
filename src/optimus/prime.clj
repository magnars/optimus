(ns optimus.prime)

(defn wrap [app get-assets optimize strategy & [options]]
  (let [f (strategy app get-assets optimize (or options {}))]
    (fn handler
      ([req] (f req))
      ([req respond raise] (f req respond raise)))))
