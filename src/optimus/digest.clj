(ns optimus.digest
  (:require [clojure.data.codec.base64 :as b64])
  (:import java.security.MessageDigest))

(def ^:private byte->hex-str
  (comp
    #(subs % 1)
    #(Integer/toString % 16)
    #(+ % 0x100)
    #(bit-and % 0xff)))

(defn- bytes->hex-str [bytes]
  (apply str
    (map byte->hex-str bytes)))

(defn sha-1 [contents]
  (bytes->hex-str
    (.digest (MessageDigest/getInstance "SHA-1")
      (.getBytes contents))))

(defn base64-string [contents]
  (String. (b64/encode (.getBytes contents)) "UTF-8"))
