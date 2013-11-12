# optimus

A Ring middleware for frontend performance optimization.

## Porcelain example

```clj
(ns my-app.example
  (require [optimus.prime :as optimus]
           [optimus.assets :as assets]
           [optimus.strategies :as strategies]))

(-> app
    (optimus/wrap
     #(concat
       (assets/load-bundle "public" "styles.css"
                           ["/styles/reset.css"
                            "/styles/main.css"])
       (assets/load-bundles "public"
                            {"lib.js" ["/scripts/angular.js"
                                       #"^/scripts/angular-ui/.+\.js$"]
                             "app.js" ["/scripts/controllers.js"
                                       "/scripts/directives.js"]})
       (assets/load-all "public" ["/images/sprites.png"
                                  "/images/bg.png"])
       [(assets/asset "/init.js" (str "var serverTime = " (.getTime (Date.)))
                      :bundle "app.js")])
     (case (:optimus-strategy env)
       :develop strategies/serve-unchanged-assets
       :debug strategies/serve-optimized-assets
       :prod strategies/serve-frozen-optimized-assets)))
```

## Design thoughts

The most basic operation of optimus is serving assets from a list, with
this basic structure:

    [{:path :contents}]

It serves the `:contents` if `:uri` in the request matches `:path`.

Built on top of that is a bunch of operations that either help you:

 - load assets to put in the list
 - optimize the assets in the list somehow
 - link to the assets

On top of the basics of `:path` and `:contents`, the asset map may contain:

 - `:bundle` - the name of the bundle this asset is part of.
 - `:headers` - headers to be served along with the asset.
 - `:original-path` - the path before any changes was made, like cache-busters.
 - `:outdated` - will only be served when referenced directly.
 - `:browsers` - will only be served to this set of browsers

Let's look at some examples.

### Loading assets

You can certainly build the list of assets manually, but there are
several ways in which optimus helpes you create the list:

 - given a path on the class path, it can read in the contents, making
   sure the file exists.

 - load a bunch of assets and assign them to a bundle in one operation

 - find files referenced in the assets you're adding (note: the
   porcelain described earlier does this for you).

It's all sugar, but it's tasty.

### Optimizing the assets

After creating the initial list of assets, you might want to tune it.
For instance:

 - bundle together assets to avoid too many HTTP requests
 - minimize JavaScript and CSS.
 - add far-future expires headers and change the path to include cache busters

### Link to the assets

After you've changed the path to a asset, or bundled a bunch together,
you need a way of referencing them in your HTML. Optimus lets you link
to assets by their original path. Or link to a bunch of assets by their
bundle name.

You don't have to serve all assets through optimus. Any paths that are
not present in the list of assets will be passed to the next ring
middleware. So you can serve additional static resources from
`compojure.route/resources` for instance.

## How to run the tests

#### Installing dependencies

We're using uglify to compress javascript. Fetch and build like so:

```
npm install
mkdir resources
./node_modules/.bin/uglifyjs --self -c -m -o resources/uglify.js
```

#### Running the tests

`lein midje` will run all tests.

`lein midje namespace.*` will run only tests beginning with "namespace.".

`lein midje :autotest` will run all the tests indefinitely. It sets up a
watcher on the code files. If they change, only the relevant tests will be
run again.

