(ns optimus.concatenate-bundles-test
  (:use [optimus.concatenate-bundles]
        [midje.sweet]))

(fact
 "When files are bundled together, we still keep the original ones.
  That way you can still cherry-pick files. However, the :bundle property
  is removed - they are no longer to be included with the bundle.

  Instead the newly created bundle file inherits the :bundle property,
  to be picked up when requested."

 (concatenate-bundles [{:path "/code.js" :original-path "/code.js" :contents "1 + 2" :bundle "app.js"}
                       {:path "/more.js" :original-path "/more.js" :contents "3 + 5" :bundle "app.js"}])
 => [{:path "/code.js" :original-path "/code.js" :contents "1 + 2"}
     {:path "/more.js" :original-path "/more.js" :contents "3 + 5"}
     {:path "/bundles/app.js"
      :contents "1 + 2\n3 + 5"
      :references nil
      :bundle "app.js"}])

(fact
 "When files are bundled together, the bundle references is a union
  of the set."

 (->> (concatenate-bundles [{:path "/main.css" :contents "" :references #{"/bg.png"}   :bundle "styles.css"}
                            {:path "/more.css" :contents "" :references #{"/logo.png"} :bundle "styles.css"}])
      (map (juxt :path :references)))

 => [["/main.css" #{"/bg.png"}]
     ["/more.css" #{"/logo.png"}]
     ["/bundles/styles.css" #{"/bg.png" "/logo.png"}]])
