# optimus

A Ring middleware for frontend performance optimization.

It serves your static assets:

 - in production: as optimized bundles
 - in development: as unchanged, individual files

In other words: Develop with ease. Optimize in production.

## Install

Add `[optimus "0.8.1"]` to `:dependencies` in your `project.clj`.

## Usage

Let's look at an example:

```clj
(ns my-app.example
  (require [optimus.prime :as optimus]
           [optimus.assets :as assets] ;; 1
           [optimus.strategies :as strategies])) ;; 2

(defn get-assets [] ;; 3
  (concat ;; 4
   (assets/load-bundle "public" ;; 5
                       "styles.css" ;; 6
                       ["/styles/reset.css" ;; 7
                        "/styles/main.css"]) ;; 8
   (assets/load-bundles "public" ;; 9
                        {"lib.js" ["/scripts/angular.js"
                                   #"/scripts/.+\.js$"] ;; 10
                         "app.js" ["/scripts/controllers.js"
                                   "/scripts/directives.js"]})
   (assets/load-assets "public" ;; 11
                       ["/images/logo.png"
                        "/images/photo.jpg"])
   [(assets/create-asset "/init.js" ;; 12
                         (str "var contextPath = " (:context-path env))
                         :bundle "app.js")]))

(-> app
    (optimus/wrap ;; 13
     get-assets ;; 14
     (case (:optimus-strategy env) ;; 15
       :develop strategies/serve-unchanged-assets ;; 16
       :prod strategies/serve-frozen-optimized-assets ;; 17
       :debug strategies/serve-optimized-assets)) ;; 18
    (ring.middleware.content-type/wrap-content-type)) ;; 19
```

1. Assets are scripts, stylesheets, images, fonts and other static
   resources your webapp uses.

2. You can choose different strategies for how you want to serve your
   assets.

3. Declare how to get your assets in a function.

4. It returns a list of assets.

5. The helpers in `optimus.assets` load files from a given directory
   on the classpath (normally in the `src/resources` directory). So in
   this case, the files are loaded from `src/resources/public/`.

6. The name of this bundle is `styles.css`.

7. It takes a list of paths. These paths double as URLs to the
   assets, and paths to the files in the public directory.

8. The contents are concatenated together in the order specified in the
   bundle.

9. You can declare several bundles at once with `load-bundles`.

10. You can use regexen to find multiple files without specifying each
    individually. Make sure you're specific enough to avoid including
    weird things out of other jars on the class path.

    Notice that `angular.js` is included first, even tho it is
    included by the regex. This how you make sure dependencies are
    loaded first.

11. You can add individual assets that aren't part of a bundle, but
     should be optimized and served through optimus. This is useful to
     add cache busters and
     [far future Expires headers](http://developer.yahoo.com/performance/rules.html#expires)
     to images served straight from your HTML.

    If you use the `optimus.assets` helpers, you don't have to list
    all images and fonts referenced in your CSS files - those are
    added along with the stylesheet.

12. The assets don't have to be files on disk. This example creates an
    asset on the path `/init.js` that is bundled along with the `app.js`
    bundle.

13. Add `optimus/wrap` as a Ring middleware.

14. Pass in the function that loads all your assets.

15. Pass in your chosen strategy. Set up properly with environment
    variables of some kind.

16. In development you want the assets to be served unchanged.

17. In production you want the assets to be optimized and frozen.

18. But there's also a strategy for debugging. What if your javascript
    doesn't minify well? How do you reproduce it? It's damn annoying
    having to restart the server for each change. Here's a strategy
    that optimizes just like production, but still serves fresh
    changes without restarts.

19. Since Ring comes with content type middleware, optimus doesn't
    worry about it. Just make sure to put it after optimus.

#### Using the new URLs

Since we're rewriting URLs to include cache busters, we need to access
them through optimus.

See example in hiccup below. Notice that we use `map`, since there is
likely more than one URL in development mode.

```cl
(ns my-app.view
  (require [optimus.link :as link]))

(defn my-page
  [request]
  (hiccup.core/html
   [:html
    [:head
     (map (fn [url] [:link {:rel "stylesheet" :href url}])
          (link/bundle-urls request ["styles.css"]))]
    [:body
     (map (fn [url] [:script {:src url}])
          (link/bundle-urls request ["lib.js" "app.js"]))]]))
```

There's also some hiccup-specific sugar:

```cl
(defn my-page
  [request]
  (hiccup.core/html
   [:html
    [:head
     (optimus.hiccup/link-to-css-bundles request ["styles.css"])]
    [:body
     (optimus.hiccup/link-to-js-bundles request ["lib.js" "app.js"])]]))
```

## So how does this work in development mode?

The given paths are used unchanged. So given this example:

```cl
(-> app
    (optimus/wrap
     #(assets/load-bundle "public" "app.js"
                          ["/app/some.js"
                           "/app/cool.js"
                           "/app/code.js"])
     strategies/serve-unchanged-assets))
```

When you call

```cl
(optimus.link/bundle-urls request ["app.js"])
```

it returns

```cl
["/app/some.js"
 "/app/cool.js"
 "/app/code.js"]
```

And those are served from `resources/public/`, or more specifically on
eg. `public/app/some.js` on the classpath.

## What about production mode?

When you use the `serve-frozen-optimized-assets` strategy, all the
contents for each bundle is read at startup. URLs are generated from
the hash of the contents and the identifier of the bundle.

So when you call `(link/bundle-urls request ["app.js"])`, it now
returns:

```cl
["/d131dd02c5e6eec4/bundles/app.js"]
```

and the middleware handles this URL by returning the concatenated
file contents in the order given by the bundle.

#### What if the contents have changed?

All the contents are read at startup, and then never checked again. To
read in new contents, the app has to be restarted.

#### No, I mean, what if someone requests an old version of app.js?

With a different hash? Yeah, then they get a 404. In production, you
should serve the files through [Nginx](http://nginx.org/) or
[Varnish](https://www.varnish-cache.org/) to avoid this problem while
doing rolling restarts of app servers.

#### Why not just ignore the hash and return the current contents?

Because then the user might be visiting an old app server with a new
URL, and suddenly she is caching stale contents. Or worse, your Nginx
or Varnish cache picks up on it and is now serving out old shit in a
new wrapping. Not cool.

This of course depends on how your machines are set up, and how you do
your rolling restarts, but it's a source of bugs that are hard to
track down.

#### What if I need to share static files with someone else?

Well, they have no way of knowing the cache buster hash, of course.
Luckily the files are still available on their original URLs.

When you're serving optimized assets, the bundles are also available. For
instance: `/d131dd02c5e6eec4/bundles/app.js` can also be accessed on
`/bundles/app.js`.

*Please note:* **You have to make extra sure these URLs are not served
with far future expires headers**, or you'll be in trouble when
updating.

#### How do I handle cache busters on images?

CSS files that reference images are rewritten so that they point to
cache busting URLs.

If you're using static images in your HTML, then you'll add a list of
these files with `optimus.assets/load-assets` like point 11 in the big
example.

And then grab the cache buster URL like so:

```cl
(link/file-path request "/images/logo.png")
```

## Can I tweak how optimus behaves?

There are some options to be tuned, but if you're planning on doing
major things there's nothing wrong with writing your own strategies or
optimizations. A pull request is welcome too.

Now, for the options. You pass them to the wrapper after the strategy:

```cl
(-> app
    (optimus/wrap
     get-assets
     the-strategy
     :cache-live-assets 2000
     :mangle-js-names true))
```

Values in this example are all defaults, so it's just a verbose noop.

- *cache-live-assets*: Assets can be costly to fetch, especially if
  you're looking up lots of different regexen on the class path.
  Considering that this has to be done for every request, it can take
  its toll on the load times in development mode.

  Tune this parameter to change for how many milliseconds the live
  assets should be frozen. `false` disabled the caching.

- *mangle-js-names*: When minifying JavaScript, local variable names
  are changed to be just one letter. This reduces file size, but
  disrupts some libraries that use clever reflection tricks - like
  Angular.JS. Set to `false` to keep local variable names intact.

## I heard rumours about support for Angular templates?

Yeah, you can use optimus to serve concatenated Angular.JS templates:

```cl
(defn get-assets []
  (concat
   (assets/load-bundles "public" my-bundles)
   [(optimus.angular/create-template-cache
     :path "/templates/angular.js"
     :module "MYAPP"
     :public-dir "public"
     :templates ["/angular/templates/home.html"
                 "/angular/templates/create.html"
                 "/angular/templates/update.html"])]))
```

This creates a file `/templates/angular.js` that inlines the templates
and adds them to the `$templateCache`.

You link to this script with:

```cl
(optimus/file-path request "/templates/angular.js")
```

Or you can add a `:bundle "app.js"` pair to the
`create-template-cache` call, and the file will be bundled together
with the rest of the javascript files in `/bundles/app.js`. Nifty.

## What are these assets anyway? They seem magical to me.

Luckily they're just data. The most basic operation of optimus is
serving assets from a list, with this minimal structure:

    [{:path :contents}]

It serves the `:contents` if the request `:uri` matches `:path`.

In addition to `:path` and `:contents`, the asset map may contain:

 - `:bundle` - the name of the bundle this asset is part of.
 - `:headers` - headers to be served along with the asset.
 - `:original-path` - the path before any changes was made, like cache-busters.
 - `:outdated` - the asset won't be linked to, but is available when referenced directly.
 - `:browsers` - the asset will only be linked to for this set of browsers *(todo)*

Built on top of that is a bunch of operations that either help you:

 - Load assets to put in the list: `optimus.assets`
 - Optimize the assets in the list somehow: `optimus.strategies`
 - Link to the assets: `optimus.link`

If you want to know more, the [tests](test/optimus) are a good place
to start reading. They go in to all the details of how optimus works
and even has some commentary on reasoning and reasons.

## How do I run the tests?

#### Installing dependencies

You need [npm](https://npmjs.org/) installed to fetch the JavaScript
dependencies. The actual fetching is automated however.

#### Running the tests

`lein midje` will run all tests.

`lein midje namespace.*` will run only tests beginning with "namespace.".

`lein midje :autotest` will run all the tests indefinitely. It sets up a
watcher on the code files. If they change, only the relevant tests will be
run again.

## License

Copyright © 2013 Magnar Sveen

Distributed under the Eclipse Public License, the same as Clojure.

