(ns optimus.time
  (:import [java.time ZonedDateTime Instant ZoneId]
           [java.time.format DateTimeFormatter]))

(def http-date-formatter (DateTimeFormatter/ofPattern "EEE, dd MMM yyyy HH:mm:ss 'GMT'"))

(def GMT (ZoneId/of "GMT"))

(defn now []
  (ZonedDateTime/now GMT))

(defn from-millis [ms]
  (ZonedDateTime/ofInstant
   (Instant/ofEpochMilli ms)
   GMT))

(defn format-http-date [zdt]
  (.format http-date-formatter zdt))
