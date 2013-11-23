(ns optimus.v8
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import [com.sun.jna WString Native Memory Pointer NativeLibrary]
           [java.io File FileOutputStream]))

(defn- find-file-path-fragments
  []
  (let [os-name (System/getProperty "os.name")
        os-arch (System/getProperty "os.arch")]
    (case [os-name os-arch]
      ["Mac OS X" "x86_64"] ["macosx/x86_64/" ".dylib"]
      ["Linux" "x86_64"]    ["linux/x86_64/" ".so"]
      ["Linux" "amd64"]     ["linux/x86_64/" ".so"]
      ["Linux" "x86"]       ["linux/x86/" ".so"]
      (throw (Exception. (str "Unsupported OS/archetype: " os-name " " os-arch))))))

(defn load-library-from-class-path
  [name path-postfix]
  (let [[binary-path binary-extension] (find-file-path-fragments)
        file-name (str name binary-extension path-postfix)
        tmp (File. (File. (System/getProperty "java.io.tmpdir")) file-name)
        lib (io/resource (str "native/" binary-path file-name))
        in (.openStream lib)
        out (FileOutputStream. tmp)]
    (io/copy in out)
    (.close out)
    (.close in)
    (System/load (.getAbsolutePath tmp))
    (.deleteOnExit tmp)))

(try
  (System/loadLibrary "v8wrapper")
  (catch UnsatisfiedLinkError e
    (load-library-from-class-path "libv8" ".clj-v8")
    (load-library-from-class-path "libv8wrapper" "")
    (System/setProperty "jna.library.path" (System/getProperty "java.io.tmpdir"))))

(def LIBRARY (com.sun.jna.NativeLibrary/getInstance "v8wrapper"))



(def run-fn (.getFunction LIBRARY "run"))
(def create-tuple-fn (.getFunction LIBRARY "create_tuple"))
(def cleanup-tuple-fn (.getFunction LIBRARY "cleanup_tuple"))

(defn create-context
  "Creates a V8 context and associated structures"
  []
  (.invokePointer create-tuple-fn (into-array [])))

(defn run-script-in-context
  "Compile and run a JS script within the given context"
  [cx script]
  (let [result (.invoke run-fn Memory (object-array [cx (new WString script)]))
        strresult (if (nil? result) nil (.getString result 0 true))]
    (when (not= (. Native getLastError) 0)
      (if (nil? result)
        (throw (Exception. "V8 reported error, but message is null!"))
        (throw (Exception. (str "V8 error: " strresult)))))
    strresult))

(defn cleanup-context
  "Cleans the memory from a context"
  [cx]
  (.invokeVoid cleanup-tuple-fn (into-array [cx])))

(defn run-script
  "Compiles and runs a JS file"
  [script]
  (let [cx (create-context)]
    (try
      (run-script-in-context cx script)
      (finally
        (cleanup-context cx)))))
