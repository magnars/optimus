(ns optimus.user-agents)

(defn ie10 [user-agent]
  (re-find #"MSIE 10\." user-agent))

(defn ie9 [user-agent]
  (re-find #"MSIE 9\." user-agent))

(defn ie8 [user-agent]
  (re-find #"MSIE 8\." user-agent))

(defn ie7 [user-agent]
  (re-find #"MSIE 7\." user-agent))

(defn ie6 [user-agent]
  (re-find #"MSIE 6\." user-agent))

(defn ie<6 [user-agent]
  (re-find #"MSIE [2345]\." user-agent))

(defn ie<7 [user-agent]
  (or (ie6 user-agent)
      (ie<6 user-agent)))

(defn ie<8 [user-agent]
  (or (ie7 user-agent)
      (ie<7 user-agent)))

(defn ie<9 [user-agent]
  (or (ie8 user-agent)
      (ie<8 user-agent)))

(defn ie<10 [user-agent]
  (or (ie9 user-agent)
      (ie<9 user-agent)))

(defn ie>9 [user-agent]
  (ie10 user-agent))

(defn ie>8 [user-agent]
  (or (ie9 user-agent)
      (ie>9 user-agent)))

(defn ie>7 [user-agent]
  (or (ie8 user-agent)
      (ie>8 user-agent)))

(defn ie>6 [user-agent]
  (or (ie7 user-agent)
      (ie>7 user-agent)))

(defn ie>5 [user-agent]
  (not (ie<6 user-agent)))

(defn ie [user-agent]
  (re-find #"MSIE " user-agent))
