(ns optimus.assets-test
  (:use [optimus.assets]
        [optimus.test-helper]
        [midje.sweet])
  (:import java.io.FileNotFoundException))

(fact
 "You can create a single asset by specifying path and contents, and
  any optional extra fields to be merged in."

 (asset "/init.js" "var x = 3"
        :bundle "app.js")

 => {:path "/init.js"
     :contents "var x = 3"
     :bundle "app.js"})

(fact
 "It requires all paths to start with a slash. Bring your own."

 (asset "init.js" "var x = 3")

 => (throws Exception "Asset paths must start with a slash. Got: init.js"))

(with-files [["/code.js" "var x = 3"]
             ["/more.js" "var y = 5"]]
  (fact
   "You can load assets from the class path. We need a public dir to
    separate 1) where your files are located, and 2) on what path you
    want to serve them."

   (load-one public-dir "/code.js")

   => {:path "/code.js"
       :contents "var x = 3"})

  (fact
   "Missing files are not tolerated."

   (load-one public-dir "/gone.js")

   => (throws FileNotFoundException "/gone.js"))

  (fact
   "To reduce verbosity, you can load several files at once."

   (load-all public-dir ["/code.js" "/more.js"])

   => [{:path "/code.js", :contents "var x = 3"}
       {:path "/more.js", :contents "var y = 5"}]))

(fact
 "File names might be garbled beyond recognition by the optimizations
  inflicted on it. We must be able to look up a file by its original
  path, so we can link to its new exciting name."

 (original-path {:path "/code-d9ar3a897d.js"
                 :original-path "/code.js"})
 => "/code.js")

(fact
 "Of course, some files might not have been changed at all. We still
  need to look them up by their 'original path'."
 (original-path {:path "/code.js"}) => "/code.js")
