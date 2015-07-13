(ns optimus.homeless)

(defn assoc-non-nil [map key val]
  (if val
    (assoc map key val)
    map))

(defn max? [vals]
  (when (seq vals)
    (apply max vals)))

(defn jdk-version []
  (read-string (System/getProperty "java.specification.version")))
