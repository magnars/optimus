# optimus [![Build Status](https://secure.travis-ci.org/magnars/optimus.png)](http://travis-ci.org/magnars/optimus)

A Ring middleware for frontend performance optimization.

It serves your static assets:

 - in production: as optimized bundles
 - in development: as unchanged, individual files

In other words: Develop with ease. Optimize in production.

## Install

Add `[optimus "0.10.0"]` to `:dependencies` in your `project.clj`.

Please note that this project uses
[Semantic Versioning](http://semver.org/). As long as we're on a `0`
major version, there will likely be API changes. Pay attention when
upgrading to a new minor version. As soon as we're on a `1` major
version, there will be no breaking changes without a major version
increase.

## Features

Depending on how you use it, optimus:

- concatenates your JavaScript and CSS files into bundles.
- minifies your JavaScript with [UglifyJS 2](https://github.com/mishoo/UglifyJS2)
- minifies your CSS with [CSSO](http://bem.info/tools/optimizers/csso/)
- adds cache-busters to your static asset URLs
- adds [far future Expires headers](http://developer.yahoo.com/performance/rules.html#expires)

Also, if you're using Angular.JS:

- prepopulates the [Angular template cache](http://docs.angularjs.org/api/ng.$templateCache) with your HTML templates.

## Usage

Let's look at an example:

```clj
(ns my-app.example
  (require [optimus.prime :as optimus]
           [optimus.assets :as assets] ;; 1
           [optimus.optimizations :as optimizations] ;; 2
           [optimus.strategies :as strategies])) ;; 3

(defn get-assets [] ;; 4
  (concat ;; 5
   (assets/load-bundle "public" ;; 6
                       "styles.css" ;; 7
                       ["/styles/reset.css" ;; 8
                        "/styles/main.css"]) ;; 9
   (assets/load-bundles "public" ;; 10
                        {"lib.js" ["/scripts/ext/angular.js"
                                   #"/scripts/ext/.+\.js$"] ;; 11
                         "app.js" ["/scripts/controllers.js"
                                   "/scripts/directives.js"]})
   (assets/load-assets "public" ;; 12
                       ["/images/logo.png"
                        "/images/photo.jpg"])
   [{:path "/init.js" ;; 13
     :contents (str "var contextPath = " (:context-path env))
     :bundle "app.js"}]))

(-> app
    (optimus/wrap ;; 14
     get-assets ;; 15
     (if (= :dev (:env config)) ;; 16
       optimizations/none ;; 17
       optimizations/all) ;; 18
     (if (= :dev (:env config)) ;; 19
       strategies/serve-live-assets ;; 20
       strategies/serve-frozen-assets)) ;; 21
    (ring.middleware.content-type/wrap-content-type)) ;; 22
```

1. Assets are scripts, stylesheets, images, fonts and other static
   resources your webapp uses.

2. You can mix and match optimizations.

3. You can choose different strategies for how you want to serve your
   assets.

4. Declare how to get your assets in a function.

5. It returns a list of assets.

6. The helpers in `optimus.assets` load files from a given directory
   on the classpath (normally in the `src/resources` directory). So in
   this case, the files are loaded from `src/resources/public/`.

7. The name of this bundle is `styles.css`.

8. It takes a list of paths. These paths double as URLs to the
   assets, and paths to the files in the public directory.

9. The contents are concatenated together in the order specified in the
   bundle.

10. You can declare several bundles at once with `load-bundles`.

11. You can use regexen to find multiple files without specifying each
    individually. Make sure you're specific enough to avoid including
    weird things out of other jars on the class path.

    Notice that `angular.js` is included first, even tho it is
    included by the regex. This way you can make sure dependencies are
    loaded before their dependents.

12. You can add individual assets that aren't part of a bundle, but
    should be optimized and served through optimus. This is useful to
    add cache busters and far future Expires headers to images served
    straight from your HTML.

    If you use the `optimus.assets` helpers, you don't have to list
    images and fonts referenced in your CSS files - those are added
    along with the stylesheet.

13. Assets don't have to be files on disk. This example creates an
    asset on the path `/init.js` that is bundled along with the `app.js`
    bundle.

14. Add `optimus/wrap` as a Ring middleware.

15. Pass in the function that loads all your assets.

16. Pass in the function that optimizes your assets. You can choose
    from those in `optimus.optimizations`, or write your own asset
    transformation functions.

17. Yeah, `optimizations/none` is basically a two-arity `identity`.

18. When you use `optimizations/all` you get everything that optimus
    provides. But you can easily exchange this for a function that
    executes only the transformations that you need.

19. Pass in your chosen strategy. Set up properly with environment
    variables of some kind.

20. In development you want the assets to be served live. No need to
    restart the app just to see changes or new files.

21. In production you want the assets to be frozen. They're loaded and
    optimized when the application starts.

    Take note: You're free to serve optimized, live assets. It'll be a
    little slow, but what if your javascript doesn't minify well? How
    do you reproduce it? It's damn annoying having to restart the
    server for each change. Here's a way that optimizes just like
    production, but still serves fresh changes without restarts.

22. Since Ring comes with content type middleware, optimus doesn't
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

#### Specifying the optimizations

If you want to mix and match optimizations, here's how you do that:

```cl
(defn my-optimize [assets options]
  (-> assets
      (optimizations/minify-js-assets options)
      (optimizations/minify-css-assets options)
      (optimizations/concatenate-bundles)
      (optimizations/add-cache-busted-expires-headers)))

(-> app
    (optimus/wrap
     get-assets
     (if (= :dev (:env config))
       optimizations/none
       my-optimize)
     the-strategy))
```

Just remember that you should always add cache busters *after*
concatenating bundles.

Adding your own asset transformation functions is fair game too. In
fact, it's encouraged. Let's say you needed to serve all assets from a
Content Delivery Network. You could do something like this:

```cl
(defn add-cdn-url-prefix [asset]
  (update-in asset [:path] #(str "http://cdn.example.com" %)))

(defn add-cdn-url-prefix-to-assets [assets]
  (map add-cdn-url-prefix-to-assets assets))

(defn my-optimize [assets options]
  (-> assets
      (optimizations/all options)
      (add-cdn-url-prefix-to-assets)))
```

This supposes that your CDN will pull assets from your app server on
cache misses. If you need to push files to a CDN, please do bother me
with an issue and I'll give it a go.

## So how does this work in development mode?

The paths are used unchanged. So given this example:

```cl
(-> app
    (optimus/wrap
     #(assets/load-bundle "public" "app.js"
                          ["/app/some.js"
                           "/app/cool.js"
                           "/app/code.js"])
     optimizations/none
     strategies/serve-live-assets))
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

When you use the `serve-frozen-assets` strategy, all the contents for
each bundle is read at startup. And with `optimizations/all`, then the
URLs are generated from the hash of the contents and the identifier of
the bundle.

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
     get-assets optimize the-strategy
     ;; options
     :cache-live-assets 2000
     :optimize-css-structure true
     :mangle-js-names true))
```

Values in this example are all defaults, so it's just a verbose noop.

- `cache-live-assets`: Assets can be costly to fetch, especially if
  you're looking up lots of different regexen on the class path.
  Considering that this has to be done for every request in
  development mode, it can take its toll on the load times.

  Tune this parameter to change for how many milliseconds the live
  assets should be frozen. `false` disables the caching.

- `optimize-css-structure`: CSSO performs structural optimizations,
  like merging blocks and removing overridden properties. Set to
  `false` to only do basic css minification.

- `mangle-js-names`: When minifying JavaScript, local variable names
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
     :templates (assets/load-assets "public"
                 ["/angular/templates/home.html"
                  "/angular/templates/create.html"
                  "/angular/templates/update.html"]))]))
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

There's also the case that some assets may be binary. Some of them
might be large. Instead of keeping those `:contents` in memory, they have
a `:get-stream` function.

Built on top of that is a bunch of operations that either help you:

 - Load assets to put in the list: `optimus.assets`
 - Optimize the assets in the list somehow: `optimus.strategies`
 - Link to the assets: `optimus.link`

If you want to know more, the [tests](test/optimus) are a good place
to start reading. They go in to all the details of how optimus works
and even has some commentary on reasoning and reasons.

## Why not split optimus into a bunch of middlewares?

I set out to create a suite of middlewares for frontend optimization.
The first was Catenate, which concerned itself with concatenation into
bundles. So I certainly agree with your point. You'd be hard pressed
to think otherwise in the Clojure community, I think, with its focus
on "decomplecting". The reason I gave up on that idea is two-fold:

 - The different optimizations are not orthogonal.
 - Assets aren't first class in the Ring middleware stack.

Some examples:

- When you bundle files together, your HTML has to reference either
  the bundle URL (in prod) or all the individual files (in dev). There
  has to be some sort of lookup from the bundle ID to a list of URLs,
  and this is dependent on your asset-serving strategy.

- When you add cache-busters to URLs, you need some sort of lookup
  from the original URL to the cache-busted URL, so you can link to
  them with a known name.

In other words, both the bundle middleware and the cache-busting
middleware either needs to own the list of assets, or it needs to rest
on a first class asset concept in the stack.

Now add the ability to serve WebP images to browsers that support it.
Not only do you have to change the image URLs, but you also have to
serve a different set of CSS to use these new images. So this
middleware would have to know which CSS files reference which files,
and rewrite them.

All of these could be fixed with a well-thought out Asset concept in
the ring middleware stack. Which is what Optimus is an attempt at. It
adds a list of assets to the request, with enough information for the
linking functions to figure out which versions of which files to link.

But then there's the orthogonality:

 - You can't add cache-busters first, and then bundle assets together,
   since you wouldn't get cache buster URLs on your bundles.

 - If you minify first, then bundle, you'll get suboptimal
   minification results in production. If you bundle first, then
   minify, you won't know which file is to blame for errors in
   development.

 - You should never add far-future expires headers unless the asset
   has a cache-buster URL.

So ordering matters. You can't just throw in another middleware, you
have to order it just so-and-so. I started writing documentation for
this in Catenate. It would say "If you're also using cache-busting
middleware, make sure to place it after Catenate." After writing a few
of those sentences, I came to the conclusion that they were not
entirely separate things. Since they're so dependent on each other,
they should live together.

There's also the case of when to optimize. In production you want to
optimize once - either as a build step, or when starting the
application. In development you don't want any optimization (unless
you're debugging), but you still need to create the list of assets so
you're able to link to it. This is something all the optimization
middlewares would have to tackle on their own - basically each layer
freezing their optimized assets on server start, and all but the last
one doing so in vain.

Optimus solves this by creating a separate middleware stack for
optimizations, that work on assets (not requests), and that can be
done at different times by different asset-serving strategies.

So yes, the optimizations have been split into several middlewares.
But not middlewares for the Ring stack. They are Asset-specific
middlewares.

For instance, even tho Optimus doesn't do transpiling, building a
transpiler to fit in the Optimus asset middleware stack is pretty
nice. You let `:original-url` be the original `"styles.less"`, so the
linking features can find it, replace the `:contents` with the
compiled CSS, and serve it under the `:path` `"styles.css"`. If your
package takes a list of assets, and returns a list of assets with all
.less files changed like this, you can plug it in with no
modifications to Optimus.

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

