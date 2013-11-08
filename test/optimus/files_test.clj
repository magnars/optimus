(ns optimus.files-test
  (:use [optimus.files]
        [optimus.test-helper]
        [midje.sweet])
  (:import java.io.FileNotFoundException))

(with-files [["/app.js" "1 + 2"]]

  (fact
   "It creates a map of basic information about the file.

    The reason it returns a list, is that one file may reference other
    files that need also be added."

   (->files public-dir "/app.js")
   => [{:path "/app.js"
        :original-path "/app.js"
        :contents "1 + 2"}])

  (fact
   "It requires all file paths to start with a slash, ie beginning at
    the root of public-dir. So bring your own slash."

   (->files public-dir "app.js") => (throws Exception "File paths must start with a slash. Got: app.js"))

  (fact
   "It will not tolerate missing files."

   (->files public-dir "/gone.js") => (throws FileNotFoundException "/gone.js")))

;; css files and their children

(with-files [["/theme/styles/main.css" "#id { background: url('../images/bg.png'); }"]
             ["/theme/images/bg.png" "binary"]]

  (fact
   "Relative URLs in css files have to be turned into absolute URLs,
    both so we can find them in the file system, but also so we can
    bundle them together at another level in the directory hierarchy."

   (->> "/theme/styles/main.css"
        (->files public-dir)
        first :contents)
   => "#id { background: url('/theme/images/bg.png'); }")

  (fact
   "Files referenced in css files must also be served. They are
    returned along with the css file."

   (->> "/theme/styles/main.css"
        (->files public-dir)
        second :path)
   => "/theme/images/bg.png"))

(with-files [["/query.css" "#id { background: url(\"/bg.png?query\"); }"]
             ["/ref.css"   "#id { background: url(/bg.png#ref); }"]
             ["/bg.png"    "binary"]]

  (fact
   "URLs can have querys and refs, but file paths can't. To find the
    files so we can serve them, these appendages have to be sliced off.

    I also threw in a test of double quotes and missing quotes on the url."

   (->> "/query.css" (->files public-dir) first :contents)
   => "#id { background: url('/bg.png'); }"

   (->> "/ref.css" (->files public-dir) first :contents)
   => "#id { background: url('/bg.png'); }"))
