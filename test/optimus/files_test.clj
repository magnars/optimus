(ns optimus.files-test
  (:use [optimus.files]
        [optimus.test-helper]
        [midje.sweet])
  (:import java.io.FileNotFoundException))

(with-files [["/app.js" "1 + 2"]]

  (fact
   "It creates a map of basic information about the file."

   (->file public-dir "/app.js")
   => {:path "/app.js"
       :original-path "/app.js"
       :contents "1 + 2"})

  (fact
   "It requires all file paths to start with a slash, ie beginning at
    the root of public-dir. So bring your own slash."

   (->file public-dir "app.js") => (throws Exception "File paths must start with a slash. Got: app.js"))

  (fact
   "It will not tolerate missing files."

   (->file public-dir "/gone.js") => (throws FileNotFoundException "/gone.js")))

;; css files and their children

(with-files [["/theme/styles/main.css" "#id { background: url('../images/bg.png'); }"]
             ["/theme/images/bg.png" "binary"]]

  (fact
   "Relative URLs in css files have to be turned into absolute URLs,
    both so we can find them in the file system, but also so we can
    bundle them together at another level in the directory hierarchy.

    The referenced files are also kept on the file map."

   (->> "/theme/styles/main.css"
        (->file public-dir))
   => {:path "/theme/styles/main.css"
       :original-path "/theme/styles/main.css"
       :contents "#id { background: url('/theme/images/bg.png'); }"
       :references #{"/theme/images/bg.png"}}))

(with-files [["/query.css" "#id { background: url(\"/bg.png?query\"); }"]
             ["/ref.css"   "#id { background: url(/bg.png#ref); }"]
             ["/bg.png"    "binary"]]

  (fact
   "URLs can have querys and refs, but file paths can't. To find the
    files so we can serve them, these appendages have to be sliced off.

    I also threw in a test of double quotes and missing quotes on the url."

   (->> "/query.css" (->file public-dir) :contents)
   => "#id { background: url('/bg.png'); }"

   (->> "/ref.css" (->file public-dir) :contents)
   => "#id { background: url('/bg.png'); }"))
