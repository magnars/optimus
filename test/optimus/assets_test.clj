(ns optimus.assets-test
  (:use [optimus.assets]
        [optimus.test-helper]
        [midje.sweet])
  (:import java.io.FileNotFoundException))

(fact
 "You can create a single asset by specifying path and contents, and
  any optional extra fields to be merged in."
 (create-asset "/init.js" "var x = 3" :bundle "app.js") => {:path "/init.js"
                                                            :contents "var x = 3"
                                                            :bundle "app.js"})

(fact
 "It requires all paths to start with a slash. Bring your own."
 (create-asset "init.js" "var x = 3") => (throws Exception "Asset paths must start with a slash. Got: init.js"))

(with-files [["/code.js" "var x = 3"]
             ["/more.js" "var y = 5"]]
  (fact
   "You can load assets from the class path. We need a public dir to
    separate 1) where your files are located, and 2) on what path you
    want to serve them."
   (load-assets public-dir ["/code.js" "/more.js"]) => [{:path "/code.js", :contents "var x = 3"}
                                                        {:path "/more.js", :contents "var y = 5"}])

  (fact
   "Missing files are not tolerated."
   (load-assets public-dir ["/gone.js"]) => (throws FileNotFoundException "/gone.js")))

(with-files [["/main.css" "#id { background: url('/bg.png'); }"]
             ["/bg.png" "binary"]]
  (fact
   "Loading a single asset is not supported, since loading an asset
    might result in more than one in the list - when the loaded asset
    in turn references more assets.

    We need to load every referenced asset at this time, since this is
    when we know where the files are located. In other words, we take
    it for granted that any files referenced in a file loaded off the
    class path are present in the same folder structure."

   (load-assets public-dir ["/main.css"]) => [{:path "/main.css"
                                               :contents "#id { background: url('/bg.png'); }"
                                               :references #{"/bg.png"}}
                                              {:path "/bg.png"
                                               :contents "binary"}]))

(with-files [["/main.css" "#id { background: url('/bg.png'); }"]]
  (fact
   "If the referenced file is not found, that too will result in a
    FileNotFoundException."

   (load-assets public-dir ["/main.css"]) => (throws FileNotFoundException "/bg.png")))

(with-files [["/theme/styles/main.css" "#id { background: url('../images/bg.png'); }"]
             ["/theme/images/bg.png" "binary"]]
  (fact
   "Relative URLs in css files have to be turned into absolute URLs,
    both so we can find them in the file system, but also so we can
    bundle them together at another level in the directory hierarchy."

   (-> (load-assets public-dir ["/theme/styles/main.css"])
       first :contents) => "#id { background: url('/theme/images/bg.png'); }"))

(with-files [["/query.css" "#id { background: url(\"/bg.png?query\"); }"]
             ["/ref.css"   "#id { background: url(/bg.png#ref); }"]
             ["/bg.png"    "binary"]]

  (fact
   "URLs can have querys and refs, but file paths can't. To find the
    files so we can serve them, these appendages have to be sliced off."

   (-> (load-assets public-dir ["/query.css"]) first :contents) => "#id { background: url('/bg.png'); }"
   (-> (load-assets public-dir ["/ref.css"]) first :contents) => "#id { background: url('/bg.png'); }"))

(fact
 "File names might be garbled beyond recognition by the optimizations
  inflicted on it. We must be able to look up a file by its original
  path, so we can link to its new exciting name."
 (original-path {:path "/code-d9ar3a897d.js" :original-path "/code.js"}) => "/code.js")

(fact
 "Of course, some files might not have been changed at all. We still
  need to look them up by their 'original path'."
 (original-path {:path "/code.js"}) => "/code.js")
