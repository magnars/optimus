(ns optimus.asset)

(defn path [{:keys [context-path path]}]
  (str context-path path))
