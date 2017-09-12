(ns optimus.optimizations.concatenate-bundles-test
  (:use [midje.sweet]
        [optimus.optimizations.concatenate-bundles]))

(fact
 "When files are bundled together, we still keep the original ones.
  That way you can still cherry-pick files. However, the :bundle property
  is removed - they are no longer to be included with the bundle.

  Instead the newly created bundle file inherits the :bundle property,
  to be picked up when requested."

 (concatenate-bundles [{:path "/code.js" :original-path "/code.js" :contents "1 + 2" :bundle "app.js"}
                       {:path "/more.js" :original-path "/more.js" :contents "3 + 5" :bundle "app.js"}])
 => [{:path "/code.js" :original-path "/code.js" :contents "1 + 2" :bundled true}
     {:path "/more.js" :original-path "/more.js" :contents "3 + 5" :bundled true}
     {:path "/bundles/app.js"
      :contents "1 + 2\n3 + 5"
      :bundle "app.js"}])

(fact
 "Files that are not in the bundle, are left alone."
 (concatenate-bundles [{:path "/code.js" :contents "1 + 2"}])
 => [{:path "/code.js" :contents "1 + 2"}])

(fact
 "When files are bundled together, the bundle references is a union
  of the set."

 (->> (concatenate-bundles [{:path "/main.css" :contents "" :references #{"/bg.png"}   :bundle "styles.css"}
                            {:path "/more.css" :contents "" :references #{"/logo.png"} :bundle "styles.css"}])
      (map (juxt :path :references)))

 => [["/main.css" #{"/bg.png"}]
     ["/more.css" #{"/logo.png"}]
     ["/bundles/styles.css" #{"/bg.png" "/logo.png"}]])

(fact
 "Bundled files keep the newest :last-modified"

 (->> (concatenate-bundles [{:path "/main.css" :contents "" :bundle "styles.css" :last-modified 1}
                            {:path "/more.css" :contents "" :bundle "styles.css" :last-modified 2}
                            {:path "/gone.css" :contents "" :bundle "styles.css"}])
      (map (juxt :path :last-modified)))
 => [["/main.css" 1]
     ["/more.css" 2]
     ["/gone.css" nil]
     ["/bundles/styles.css" 2]])

(fact
 "Bundle files under a custom URL prefix"

 (->> (concatenate-bundles [{:path "/main.css" :contents "" :bundle "styles.css"}]
                           {:bundle-url-prefix "/assets"})
      (map :path))
 => ["/main.css"
     "/assets/styles.css"])

(fact
 "Throws when bundling assets with different context-paths"

 (concatenate-bundles [{:path "/main.css" :contents "" :bundle "styles.css" :context-path "/test"}
                       {:path "/other.css" :contents "" :bundle "styles.css" :context-path "/lol"}]
                      {:bundle-url-prefix "/assets"})
 => (throws Exception))

(fact
 "Throws when bundling assets with different base-urls"

 (concatenate-bundles [{:path "/main.css" :contents "" :bundle "styles.css" :base-url "http://cdn"}
                       {:path "/other.css" :contents "" :bundle "styles.css" :base-url "http://cdn2"}]
                      {:bundle-url-prefix "/assets"})
 => (throws Exception))
