(ns optimus.assets-test
  (:use [optimus.assets]
        [test-with-files.core]
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
   (load-assets public-dir ["/code.js" "/more.js"]) => [{:path "/code.js", :contents "var x = 3", :last-modified *last-modified*}
                                                        {:path "/more.js", :contents "var y = 5", :last-modified *last-modified*}])

  (fact
   "Missing files are not tolerated."
   (load-assets public-dir ["/gone.js"]) => (throws FileNotFoundException "/gone.js")))

(defn- plausible-timestamp? [actual-timestamp]
  (let [upstream-timestamp 1384517932000
        twenty-four-hours (* 24 3600 1000)]
    (< (- upstream-timestamp twenty-four-hours)
       actual-timestamp
       (+ upstream-timestamp twenty-four-hours))))

(fact
 "Files in JARs report the correct last-modified time."

 (map (juxt :path :last-modified) (load-assets "optimus-test-jar" ["/blank.gif"]))
 => (just (just ["/blank.gif" plausible-timestamp?])))

(with-files [["/styles/reset.css" ""]
             ["/styles/main.css" ""]
             ["/external/kalendae.css" ""]]
  (fact
   "You can load multiple assets using regex."

   (->> (load-assets public-dir [#"/styles/.+\.css$"])
        (map :path)
        (set)) => #{"/styles/reset.css"
                    "/styles/main.css"})

  (fact
   "If no files match the regex, you want to know."

   (load-assets public-dir [#"/stlyes/.+\.css%"]) => (throws Exception "No files matched regex /stlyes/.+\\.css%"))

  (fact
   "If you need the files in a specific order, you can list the
    ordered ones first."

   (->> (load-assets public-dir ["/styles/reset.css"
                                 #"/styles/.+\.css$"])
        (map :path)) => ["/styles/reset.css"
                         "/styles/main.css"])

  (fact
   (->> (load-assets public-dir ["/styles/main.css"
                                 #"/styles/.+\.css$"])
        (map :path)) => ["/styles/main.css"
                         "/styles/reset.css"]))

(fact
 "Emacs file artifacts are ignored by the regex matcher."

 (with-files [["/app/code.js" ""]
              ["/app/#code.js#" ""]
              ["/app/.#code.js" ""]]

   (->> (load-assets public-dir [#"/app/*"])
        (map :path)) => ["/app/code.js"]))

(fact
 "Loading a single asset is not supported, since loading an asset
  might result in more than one in the list - when the loaded asset in
  turn references more assets.

  We need to load every referenced asset at this time, since this is
  when we know where the files are located. In other words, we take it
  for granted that any files referenced in a file loaded off the class
  path are present in the same folder structure."

 (with-files [["/main.css" "#id { background: url('/bg.png'); }"]
              ["/bg.png" "binary"]]
   (->> (load-assets public-dir ["/main.css"])
        (map #(select-keys % #{:path :references}))) => [{:path "/main.css" :references #{"/bg.png"}}
                                                         {:path "/bg.png"}]))

(fact
 "Imports are supported, in that they are recognized as references."

 (with-files [["/main.css" "@import url('other.css');"]
              ["/other.css" "#id { background: url('bg.png'); }"]
              ["/bg.png" "binary"]]
   (->> (load-assets public-dir ["/main.css"])
        (map #(select-keys % #{:path :references}))) => [{:path "/main.css" :references #{"/other.css"}}
                                                         {:path "/other.css" :references #{"/bg.png"}}
                                                         {:path "/bg.png"}])

 (with-files [["/main.css" "@import 'other.css';"]
              ["/other.css" "#id {}"]]
   (->> (load-assets public-dir ["/main.css"])
        (map #(select-keys % #{:path :references}))) => [{:path "/main.css" :references #{"/other.css"}}
                                                         {:path "/other.css"}]))

(with-files [["/main.css" "#id { background: url(data:image/gif;base64,R0lGODlhAQABAAAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw==)}"]]
  (fact
   "Data URLs are left alone."

   (-> (load-assets public-dir ["/main.css"])
       first :contents) => "#id { background: url(data:image/gif;base64,R0lGODlhAQABAAAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw==)}"))

(with-files [["/main1.css" "#id { background: url(//some.site/img.png)}"]
             ["/main2.css" "#id { background: url(http://some.site/img.png)}"]
             ["/main3.css" "#id { background: url(https://some.site/img.png)}"]]
  (fact
   "External URLs are left alone."

   (->> (load-assets public-dir ["/main1.css" "/main2.css" "/main3.css"])
        (map (juxt :path :contents))) => [["/main1.css" "#id { background: url(//some.site/img.png)}"]
                                          ["/main2.css" "#id { background: url(http://some.site/img.png)}"]
                                          ["/main3.css" "#id { background: url(https://some.site/img.png)}"]]))

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

(fact
 "URLs can have querys and refs, but file paths can't. To find the
  files so we can serve them, these appendages have to be sliced off.

  Because they're needed for weird bugs with @font-face in certain
  browsers tho, we need to preserve them in the actual URL."

 (with-files [["/query.css" "#id { background: url(\"/bg.png?query\"); }"]
              ["/ref.css"   "#id { background: url(/bg.png#ref); }"]
              ["/bg.png"    "binary"]]
   (->> (load-assets public-dir ["/query.css" "/ref.css"])
        (map (juxt :contents :references))
        (set)) => #{["#id { background: url(\"/bg.png?query\"); }" #{"/bg.png"}]
                    ["#id { background: url(/bg.png#ref); }" #{"/bg.png"}]
                    [nil nil]}))

(with-files [["/code.js" "1 + 2"]
             ["/more.js" "3 + 5"]]
  (fact
   "load-bundle is like load-assets, except it sets the :bundle
    property - allowing the files to be concatenated later. The order
    is preserved to ensure files in the bundle are loaded in the
    correct sequence."

   (load-bundle public-dir "app.js" ["/code.js" "/more.js"]) => [{:path "/code.js"
                                                                  :contents "1 + 2"
                                                                  :last-modified *last-modified*
                                                                  :bundle "app.js"}
                                                                 {:path "/more.js"
                                                                  :contents "3 + 5"
                                                                  :last-modified *last-modified*
                                                                  :bundle "app.js"}])

  (fact
   "Files matched with a regexp are also part of the bundle."

   (set (map :path (load-bundle public-dir "app.js" [#"/.+\.js$"])))) => #{"/code.js" "/more.js"}

   (fact
    "There's load-bundles to reduce verbosity."

    (load-bundles public-dir {"lib.js" ["/code.js"]
                              "app.js" ["/more.js"]}) => [{:path "/code.js"
                                                           :contents "1 + 2"
                                                           :last-modified *last-modified*
                                                           :bundle "lib.js"}
                                                          {:path "/more.js"
                                                           :contents "3 + 5"
                                                           :last-modified *last-modified*
                                                           :bundle "app.js"}]))

(with-files [["/main.css" "#id { background: url('/bg.png'); }"]
             ["/bg.png" "binary"]]
  (fact
   "Referenced files are not part of the bundle."

   (->> (load-bundle public-dir "styles.css" ["/main.css"])
        (map (juxt :path :bundle))) => [["/main.css" "styles.css"]
                                        ["/bg.png" nil]]))

(fact
 "File names might be garbled beyond recognition by the optimizations
  inflicted on it. We must be able to look up a file by its original
  path, so we can link to its new exciting name."
 (original-path {:path "/code-d9ar3a897d.js" :original-path "/code.js"}) => "/code.js")

(fact
 "Of course, some files might not have been changed at all. We still
  need to look them up by their 'original path'."
 (original-path {:path "/code.js"}) => "/code.js")

(fact
 "If you're listing out a bunch of files in the same folders, it can
  be useful to extract the common prefix, if only to preserve your
  sanity."

 (with-prefix "/scripts/angular/"
   ["some.js"
    "more.js"
    "code.js"]) => ["/scripts/angular/some.js"
                    "/scripts/angular/more.js"
                    "/scripts/angular/code.js"])
