# <img align="right" src="optimus.png"> Optimus

A Ring middleware for frontend performance optimization.

It serves your static assets:

 - in production: as optimized bundles
 - in development: as unchanged, individual files

In other words: Develop with ease. Optimize in production.

## Features

Depending on how you use it, Optimus:

- concatenates your JavaScript and CSS files into bundles.
- adds cache-busters to your static asset URLs
- adds [far future Expires headers](http://developer.yahoo.com/performance/rules.html#expires)
- minifies your JavaScript with [UglifyJS 2](https://github.com/mishoo/UglifyJS2)
- minifies your CSS with [clean-css](https://github.com/jakubpawlowicz/clean-css)
- inlines CSS imports while preserving media queries

You might also be interested in:

- [optimus-autoprefixer](https://github.com/magnars/optimus-autoprefixer) - an
  asset middleware which uses
  [autoprefixer](https://github.com/postcss/autoprefixer) to add vendor prefixes
  to CSS files.
- [optimus-angular](http://github.com/magnars/optimus-angular) - which
  comes with a custom asset loader that prepopulates the Angular.JS
  template cache. It also has Optimus asset middleware that prepares
  Angular.JS code for minification.
- [optimus-less](https://github.com/magnars/optimus-less) - which adds
  a custom asset loader for [LESS](http://lesscss.org/) files.
- [optimus-sass](https://github.com/DomKM/optimus-sass) - which adds
  a custom asset loader for [Sass/SCSS](http://sass-lang.com/) files.
- [optimus-coffeescript](https://github.com/alexjg/optimus-coffeescript) - which adds a custom asset loader for [CoffeeScript](http://coffeescript.org/) files.
- [optimus-jsx](https://github.com/magnars/optimus-jsx) - which adds a
  custom asset loader for
  [React JSX](http://facebook.github.io/react/docs/jsx-in-depth.html)
  files.
- [optimus-img-transform](http://github.com/magnars/optimus-img-transform) - an
  asset middleware to transform your images' size, quality and rendering methods.

## Install

Add `[optimus "2025.01.19.2"]` to `:dependencies` in your `project.clj`.

This project no longer uses Semantic Versioning. Instead we're aiming to never
break the API. Feel free to check out the [change log](#change-log).

There were breaking changes in `0.16`, `0.17`, `0.19` and `2022-02-13`. If
you're upgrading, you might want to [read more about them](breaking-changes.md).

## Usage

Let's look at an example:

```clj
(ns my-app.example
  (:require [optimus.prime :as optimus]
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
    (ring.middleware.content-type/wrap-content-type) ;; 22
    (ring.middleware.not-modified/wrap-not-modified)) ;; 23
```

1. Assets are scripts, stylesheets, images, fonts and other static
   resources your webapp uses.

2. You can mix and match optimizations.

3. You can choose different strategies for how you want to serve your
   assets.

4. Declare how to get your assets in a function.

5. It returns a list of assets.

6. The helpers in `optimus.assets` load files from a given directory
   on the classpath (normally in the `resources` directory). So in
   this case, the files are loaded from `resources/public/`.

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
    should be optimized and served through Optimus. This is useful to
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

17. `optimizations/none` does nothing and returns your assets unharmed.

18. When you use `optimizations/all` you get everything that Optimus
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

22. Since Ring comes with content type middleware, Optimus doesn't
    worry about it. Just make sure to put it after Optimus.

23. The same goes for responding with `304 Not Modified`. Since
    Optimus adds `Last-Modified` headers, Ring handles the rest.

#### Using the new URLs

Since we're rewriting URLs to include cache busters, we need to access
them through Optimus.

Notice that we use `map`, since there is likely more than one URL in development
mode.

```cl
(ns my-app.view
  (:require [optimus.link :as link]))

(defn my-page
  [request]
  (hiccup.core/html
   [:html
    [:head
     (map (fn [url] [:link {:rel "stylesheet" :href url}])
          (link/bundle-paths request ["styles.css"]))]
    [:body
     (map (fn [url] [:script {:src url}])
          (link/bundle-paths request ["lib.js" "app.js"]))]]))
```

There's even some sugar available:

```cl
(defn my-page
  [request]
  (hiccup.core/html
   [:html
    [:head
     (optimus.html/link-to-css-bundles request ["styles.css"])]
    [:body
     (optimus.html/link-to-js-bundles request ["lib.js" "app.js"])]]))
```

These `link-to-*-bundles` will return a string of HTML that includes several
script/link tags in development, and a single tag in production.

#### Specifying the optimizations

If you want to mix and match optimizations, here's how you do that:

```cl
(defn my-optimize [assets options]
  (-> assets
      (optimizations/minify-js-assets options)
      (optimizations/minify-css-assets options)
      (optimizations/inline-css-imports)
      (optimizations/concatenate-bundles)
      (optimizations/add-cache-busted-expires-headers)
      (optimizations/add-last-modified-headers)))

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
fact, it's encouraged. Let's say you need to serve all assets from a
Content Delivery Network ...

#### Yeah, we are using a Content Delivery Network. How does that work?

To serve the files from a different host, add a `:base-url` to the assets:

```cl
(defn add-cdn-base-url-to-assets [assets]
  (map #(assoc % :base-url "http://cdn.example.com") assets))

(defn my-optimize [assets options]
  (-> assets
      (optimizations/all options)
      (add-cdn-base-url-to-assets)))
```

This supposes that your CDN will pull assets from your app server on
cache misses. If you need to push files to the CDN, you also need to
save them to disk. Like this:

```cl
(defn export-assets []
  (-> (get-assets)
      (my-optimize (get-optimisation-options))
      (optimus.export/save-assets "./cdn-export/")))
```

You can even add an alias to your `project.clj`:

```cl
:aliases {"export-assets" ["run" "-m" "my-app.example/export-assets"]}
```

And run `lein export-assets` from the command line. Handy.

#### Hey, I'm serving my app from a sub-directory, how can I avoid referencing it everywhere?

Locally, your app is known as `http://localhost:3000/`, but sometimes in
production it must share the limelight with others like it. Maybe it'll go live
at `http://limelight.com/myapp/`. Wouldn't it be nice if you could do that without
adding the extra folder and referencing it everywhere?

To serve the files from a directory/context path add a `:context-path` to the
assets:

```cl
(defn add-context-path-to-assets [assets]
  (map #(assoc % :context-path "/myapp/") assets))

(defn my-optimize [assets options]
  (-> assets
      (add-context-path-to-assets)
      (optimizations/all options)))
```

Now your links to your assets (including those in CSS files) will reference
assets with the context path + the file path.

(Note that you need to add context paths *before* optimizing assets, otherwise
absolute references in CSS files will not include the context path).

#### Those are a whole lot of files being exported.

Yeah, two reasons for that:

- Optimus supports linking to individual assets even after they're
  bundled. If you don't want that, remove the `:bundled` assets.

- Optimus supports linking to assets by their original URL. If there
  are no external apps that need to link to your assets, remove the
  `:outdated` assets.

Like this:

```cl
(defn export-assets []
  (as-> (get-assets) assets
        (optimizations/all assets options)
        (remove :bundled assets)
        (remove :outdated assets)
        (optimus.export/save-assets assets "./cdn-export/")))
```

#### But now it takes more time when the app starts in production!

Yes, doing work takes time. If you want to avoid optimizing all the assets on
every app startup, you can do it during build time. The build server will build
and optimize the assets, write the assets to disk, and also write an asset
manifest that your app will read during startup. Your uberjar should include
the assets and the asset manifest.

Now, hold tight while I dump this code on you. You've already seen bits and
pieces of it above. The `build` and `get-prebuilt-assets` functions are the two
sister functions that work in tandem to produce and consume the asset manifest
during build and runtime, respectively:

```cl
(ns my-app.assets
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as string]
    [optimus.export]
    [optimus.optimizations]))

;; Functions used during build

(defn get-optimized-assets [get-assets-fn optimize-assets-fn]
  (->> (get-assets-fn)
       (optimize-assets-fn)
       (into []
             (comp (remove :outdated)
                   (remove :bundled)))))

(defn get-asset-manifest [assets]
  (for [asset assets] (dissoc asset :contents :resource)))

(defn build
  "Build and optimize assets using Optimus. Saves the optimized assets to the target dir, and writes an
   asset-manifest.edn to the target dir."
  [get-assets-fn optimize-assets-fn target]
  (let [optimized (get-optimized-assets get-assets-fn optimize-assets-fn)]
    (optimus.export/save-assets optimized target)
    (spit (str target "/asset-manifest.edn")
          (pr-str (get-asset-manifest optimized)))))

;; Functions used during app startup

(defn add-asset-resource [asset]
  (if-let [resource (-> (:path asset)
                        (string/replace-first \/ "")
                        (io/resource))]
    (assoc asset :resource resource)
    (throw (ex-info "Could not read resource for asset" {:asset asset}))))

(defn get-prebuilt-assets
  "Reads the asset-manifest.edn file and returns a vector of Optimus bundle assets to be used by Optimus middleware.

   Throws if asset-manifest.edn or any of the referenced assets couldn't be read."
  []
  (if-let [assets (edn/read-string (slurp (io/resource "asset-manifest.edn")))]
    (mapv add-asset-resource assets)
    (throw (Exception. "Could not read assets from asset-manifest.edn"))))

;; Your app-specific assets and optimizations

(defn get-assets []
  ;; Your vanilla get-assets function, used for dev and prod both.
  )

(defn optimize-assets [assets]
  ;; Your production optimizations
  (-> assets
      (optimus.optimizations/all {})))
      
(defn build-assets!
  [target-dir]
  (println "Building assets into" target-dir)
  (build get-assets optimize-assets target-dir))
```

From Leiningen you can build your assets like this:

1. Have a task alias that invokes the `build-assets!` function. In your
`project.clj`:
    ```clojure
      :profiles {:uberjar {:resource-paths ["build-resources"]}}
      :aliases {"build-assets" ["run" "-m" "my-app.assets/build-assets!" "build-resources"]}
    ```

1. Augment the command for building your uberjar thus:
    ```clojure
    lein do clean, build-assets, uberjar
    ```

If you're using [tools.build](https://clojure.org/guides/tools_build) you must
call `build-assets!` from your uberjar-building function.

That was the build. Now your optimized bundles are part of the uberjar. During
startup your app must now read the asset manifest instead of going off and
optimizing all the assets all over again. So the middleware must be set up
somewhat differently than what we have seen so far:

```clojure
(optimus/wrap
  (if (= :dev (:env config)) ;; 1
    assets/get-assets
    assets/get-prebuilt-assets)
  optimus.optimizations/none ;; 2
  (if (= :dev (:env config)) ;;3
    optimus.strategies/serve-live-assets
    optimus.strategies/serve-frozen-assets))
```

1. In prod, get the assets from the asset manifest.
2. Never optimize anything - prod assets will be pre-optimized, and in dev you
don't want to optimize.
3. Choose you strategy - this is the same as above.

#### Serving bundles under a custom URL prefix

`optimizations/concatenate-bundles` generates a bundled asset with the
`"/bundle"` prefix. If you need to serve assets with a different prefix, provide
the `:bundle-url-prefix` config option to either `optimizations/all` or
`optimizations/concatenate-bundles`:

```cl
(-> app
    (optimus/wrap
     get-assets
     (optimizations/all {:bundle-url-prefix "/assets/bundles"})
     the-strategy))
```

## So how does all this work in development mode?

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
(optimus.link/bundle-paths request ["app.js"])
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
each bundle is read at startup. And with `optimizations/all`, the URLs
are generated from the hash of the contents and the identifier of the
bundle.

So when you call `(link/bundle-paths request ["app.js"])`, it now
returns:

```cl
["/bundles/d131dd02c5e6/app.js"]
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
instance: `/bundles/d131dd02c5e6/app.js` can also be accessed on
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

You can also add a fallback image, if the given one doesn't exists.

```cl
(link/file-path request (str "/images/members/" (:id member) ".png")
                :fallback "/images/anonymous.png")
```

[Sam Ritchie](https://github.com/sritchie) has written
[this HTML transformation](https://gist.github.com/sritchie/7794646)
using [Enlive](https://github.com/cgrand/enlive) that rewrites all
your image tags with no extra work. That is pretty cool!

## Can I tweak how Optimus behaves?

There are some options to be tuned, but if you're planning on doing
major things there's nothing wrong with writing your own strategies or
optimizations. A pull request is welcome too.

Now, for the options. You pass them to the wrapper after the strategy:

```cl
(-> app
    (optimus/wrap
     get-assets optimize the-strategy
     {:cache-live-assets 2000
      :uglify-js {:mangle-names true
                  :transpile-es6? false}
      :clean-css {:level 2}}))
```

Values in this example are all defaults, so it's just a verbose noop.

- `:cache-live-assets` - Assets can be costly to fetch, especially if
  you're looking up lots of different regexen on the class path.
  Considering that this has to be done for every request in
  development mode, it can take its toll on the load times.

  Tune this parameter to change for how many milliseconds the live
  assets should be frozen. `false` disables the caching.

#### `:uglify-js`

- `:mangle-names` - When minifying JavaScript, local variable names
  are changed to be just one letter. This reduces file size, but disrupts some
  libraries that use clever reflection tricks - like Angular.JS. Set to `false`
  to keep local variable names intact.

- `:transpile-es6?` - UglifyJS does not support the new syntax in ES6. Set this
  to `true` to let Babel transpile ES6 code into ES5 before minification.

#### `:clean-css`

These options are passed straight to clean-css. Please see the [clean-css
documentation](https://github.com/clean-css/clean-css#constructor-options) for
available options.

In earlier versions of Optimus, this was a [curated set of
options](old-clean-css.md). These old options will still work (we're trying not
to break your stuff), but it is probably a good idea to take a look at all the
available settings in clean-css.

## Automatic compilation when assets source files change

Sometimes in development mode serving live assets may be too slow even with caching
(for example if you're looking up lots of regexen on the class path).
In this case you can use `serve-live-assets-autorefresh` strategy.
This strategy will watch for changes in assets source files and
recompile assets in the background whenever it's needed.
Compiled assets are then cached until the next change in the source files.
By default it assumes that assets sources are located in `resources` directory,
but it can be customized with `:assets-dirs` config option.

```cl
(-> app
    (optimus/wrap
     get-assets optimize serve-live-assets-autorefresh
     {:assets-dirs ["resources/public"]}))
```

## What are these assets anyway? They seem magical to me.

Luckily they're just data. The most basic operation of Optimus is
serving assets from a list, with this minimal structure:

    [{:path :contents}]

It serves the `:contents` if the request `:uri` matches `:path`.

In addition to `:path` and `:contents`, the asset map may contain:

 - `:bundle` - the name of the bundle this asset is part of.
 - `:headers` - headers to be served along with the asset.
 - `:original-path` - the path before any changes was made, like cache-busters.
 - `:outdated` - the asset won't be linked to, but is available when referenced directly.
 - `:base-url` - prepended to the path when linking.
 - `:last-modified` - when the asset was last modified, in milliseconds since epoch.

There's also the case that some assets may be binary. Some of them
might be large. Instead of keeping those `:contents` in memory, they have
a `:resource` instead, which is a URL to be served as a stream.

Built on top of that is a bunch of operations that either help you:

 - Load assets to put in the list: `optimus.assets`
 - Optimize the assets in the list somehow: `optimus.optimizations`
 - Decide how you want to serve the assets: `optimus.strategies`
 - Link to the assets: `optimus.link`

If you want to know more, the [tests](test/optimus) are a good place
to start reading. They go in to all the details of how Optimus works
and even has some commentary on reasoning and reasons.

## Are there any working examples to look at?

Take a look at these:

 - The [emacsrocks.com](http://emacsrocks.com) and
   [parens-of-the-dead.com](http://www.parens-of-the-dead.com/) websites both
   use Optimus to optimize their assets. Source
   [here](https://github.com/magnars/emacsrocks.com) and
   [here](https://github.com/magnars/www.parens-of-the-dead.com).

 - [August Lilleaas](http://augustl.com) wrote
   [a blog](http://augustl.com/blog/2014/jdk8_react_rendering_on_server/)
   about using React on the server with JDK8. In
   [the example code](https://github.com/augustl/react-nashorn-example),
   he uses Optimus for frontend optimization.

 - The [sinonjs.org](http://sinonjs.org) site is a static website
   written with [Stasis](https://github.com/magnars/stasis), and it
   too uses Optimus for frontend optimization. Here's the
   [sinon-docs repo](https://github.com/sinonjs/sinon-docs).

Are you using Optimus in an open-source project? Please do let me
know, and I'll add it to the list.

## Why not split Optimus into a bunch of middlewares?

I set out to create a suite of middlewares for frontend optimization.
The first was Catenate, which concerned itself with concatenation into
bundles. So I certainly agree with your point. You'd be hard pressed
to think otherwise in the Clojure community, I think, with its focus
on "decomplecting". The reason I gave up on that idea is two-fold:

 - Assets aren't first class in the Ring middleware stack.
 - The different optimizations are not orthogonal.

I'll try to elaborate.

#### First class assets

Let's look at two examples:

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
the Ring middleware stack. Which is what Optimus is an attempt at. It
adds a list of assets to the request, with enough information for the
linking functions to figure out which versions of which files to link.

#### Orthogonality

Different transformations aren't orthogonal. Some examples:

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

#### Optimus' solution

Optimus solves this by creating a separate middleware stack for
optimizations, that work on assets (not requests), and that can be
done at different times by different asset-serving strategies.

So yes, the optimizations have been split into several middlewares.
But not middlewares for the Ring stack. They are Asset-specific
middlewares.

#### Optimus is open for extension

Even tho Optimus itself doesn't do transpiling, building a transpiler
to use with Optimus is pretty nice. I created
[optimus-less](https://github.com/magnars/optimus-less) as an example
implementation.

You create a custom asset loader and plug it into Optimus'
`load-asset` multimethod. Let `:original-url` be the original
`"styles.less"`, so the linking features can find it, replace the
`:contents` with the compiled CSS, and serve it under the `:path`
`"styles.css"`.

It's about
[20 lines of code](https://github.com/magnars/optimus-less/blob/master/src/optimus_less/core.clj),
including requires. And adding support for more transpilers require no
changes to Optimus itself.

## Can I switch JavaScript engines?

Yes.

Optimus relies on the `javax.script` (JSR-223) API to load a JS engine from the
classpath. Optimus only adds GraalJS to the classpath, as an explicit dependency, to
ensure there is a sensible default.

So it is possible to swap out GraalJS for any other `javax.script`-compatible JS engine
that is available on the classpath. Simply add your alternative JS engine as a dependency
using your project's build system. (Note: if doing so, you may wish to add Optimus'
GraalJS dependencies to its `:exclusions`.)

Once another JS engine is made available to `javax.script`, Optimus can be instructed to
prefer it using the [environ](https://github.com/weavejester/environ) setting
`:optimus-js-engines`.

This can be declared in various possible ways:

- Using Leiningen: add `lein-environ` as a plugin. Then add `:env {:optimus-js-engines
  "my-engine-name"}` to `project.clj` (or other source that gets merged into the project
  map).
- Using Boot: add `boot-environ` as a dependency and invoke its `environ` task, passing
  `:env {:optimus-js-engines "my-engine-name"}`.
- Using Java system properties, without dependencies, pass
  `-Doptimus.js.engines=my-engine-name`.
- Using the shell's environment, ensure the variable `OPTIMUS_JS_ENGINES=my-engine-name`
  is set for the Java process running Optimus.

The value `my-engine-name` can be a single preferred engine, or a comma-separated list of
engine names, e.g. `nashorn,rhino`, ordered left-to-right from most-preferred to
least-preferred. Only the first, most-preferred available engine gets loaded and used.

### Example: Rhino

To use Rhino, first add a dependency on a JSR-223 API for Rhino such as
`[cat.inspiracio/rhino-js-engine "1.7.10"]` (this uses
[bunkenberg/rhino-js-engine](https://bitbucket.org/bunkenburg/rhino-js-engine/)).

Then if using Leiningen, add `{:env {:optimus-js-engines "rhino"}}` to `project.clj`. (You
may want to add this to a profile, e.g. if switching between engines.)

Alternatively, define a Java system property in your run context, such as
`-Doptimus.js.engines=rhino`. Or set the shell environment variable
`OPTIMUS_JS_ENGINES=rhino` for the execution context.

### Example: Nashorn

_Warning: Nashorn is slow and deprecated beyond Java 9._

Using a suitable JDK that ships with Nashorn, just declare:

- `:env {:optimus-js-engines "nashorn"}` in Leiningen with the `lein-environ` plugin, or
- `:env {:optimus-js-engines "nashorn"}` in Boot with the `environ` task from
  `boot-environ`, or
- `-Doptimus.js.engines=nashorn` as a Java system property, or
- `export OPTIMUS_JS_ENGINES=nashorn` in the shell context.

### What about V8 or other engines?

In past Optimus versions before we switched to `javax.script`, V8 was loaded using clj-v8,
but clj-v8 does not expose a `javax.script` API. At the time of writing, there is no
maintained V8 binding for general-purpose use from Java, let alone a maintained
`javax.script` binding for V8.

If such a project should emerge, it should be possible to add it to your dependencies and
declare its short name in `:optimus-js-engines` to load it, without any changes to Optimus
itself.

Likewise, for any other JS engine that implements `javax.script` interfaces.


## Change log

There were breaking changes in `0.16`, `0.17` and `0.19`. If you're upgrading,
you might want to [read more about them](breaking-changes.md).

#### From 2023.11.21 to 2025.01.19.2

- Switch to a branch of clean-css to support transition-behavior
- Avoid private functions, no need to hoard useful stuff
- Make available `assets/get-contents` convenience function
- Make available `assets/get-asset-by-path` convenience function

#### From 2023.10.13 to 2023.11.21

- Update clean-css to 5.3.2

     This includes exposing all the clean-css options. The legacy option format
     is still supported, but switching to the new one is recommended. [Read more](old-clean-css.md)

- Heuristic for detecting already minified assets reduced.

      This used to be 5000 chars on a single line, now reduced to 1000 chars.
      This due to smaller clojurescript projects not needing 5000 chars in
      total. We're hoping people don't write 1000 chars wide lines manually.

- Support directly using optimized path in link/file-path

      In case you already have the optimized path for some reason, this will no
      longer trip you up.

#### From 2023-10-03 to 2023.10.13

- Add option for multiple :assets-dirs (used by live reload)

    The old :assets-dir is still supported for backwards compatibility, but no
    longer documented.

#### From 2023-02-08 to 2023-10-03

- Use nextjournal/beholder instead of juxt/dirwatch for a faster live assets autorefresh strategy. ([Christian Johansen](https://github.com/cjohansen))

#### From 2022-02-13 to 2023-02-08

- Added option to transpile JS from ES6 to ES5 with Babel before running UglifyJS. ([the-exodus](https://github.com/the-exodus))
- Bumped java.data dependency

#### From 0.20.2 to 2022-02-13

- Pluggable JS engine via JSR223. Execute JS optimizations w/ GraalJS, Nashorn or Rhino (radhika reddy)

  This means that the dependency on clj-v8 and its V8 binaries are gone. This allows us to:

- Add support for Windows (August Lilleaas)

This is what we have been waiting for to release the big 1.0, except that we [no
longer belive in semantic
versioning](https://www.youtube.com/watch?v=oyLBGkS5ICk) - or to be more
precise: We aim to never again break the API, removing the need.

Also:

 - removed clj-time and Joda
 - bumped dependency versions


#### From 0.20.0 to 0.20.2

- Respect context-path in export (Christian Johansen)
- Allow for spaces in url( ... ) in CSS

#### From 0.19.0 to 0.20

- Use a fixed version of clj-v8 that does not crash when starting two processes
  using it at the same time.
- Fixed an issue with files referenced in CSS files when using `:base-url`. (Luke Snape)
- Added support for custom `:bundle-url-prefix` (Christian Johansen)
- Added support for `:context-path` per asset (Christian Johansen)

#### From 0.18.5 to 0.19

- We have updated to Clojure 1.8.0 and now require JDK 1.7 or higher.
- Fixed loading issues with `serve-live-assets-autorefresh`

#### From 0.18 to 0.18.5

- Like CSS before, JS-files with a single line over 5000 characters are now
  considered already minified, and skipped.

- Avoid memory leak via clj-v8 contexts (Allen Rohner)

- We now designate specific versions of our JavaScript dependencies, avoiding
  random breakage when these packages change.

#### From 0.17 to 0.18

- A new strategy `serve-live-assets-autorefresh` is available. It watches for
  changes in assets source files and recompiles assets in the background
  whenever it's needed.
- CSS files now adopt the newest modification time of its imports
- Now doesn't fail when given outdated MicroSoft behavior CSS.

#### From 0.16 to 0.17

- **The optimus.hiccup namespace is removed in favor of optimus.html**

  The old `optimus.hiccup/link-to-css-bundles` created hiccup data structures,
  which is a mistake. It's specific to one rendering method, and Hiccup works
  just as well (or better) with strings.

  If you were using `optimus.hiccup`, just replace it will `optimus.html` and
  all is well.

#### From 0.15 to 0.16

- **Optimus now uses clean-css instead of CSSO for minification.**

  CSSO was abandoned along with quite a few bugs.
  [clean-css](https://github.com/jakubpawlowicz/clean-css) is faster, has fewer
  bugs, a comprehensive test suite, and is under active development.

  See [the new customization options for clean-css](#can-i-tweak-how-optimus-behaves)

- **Passing options to optimus.prime/wrap now uses a regular map**

  It used to take syntactic sugar varargs, but this actually makes it harder to
  use in practice. You now just pass in a map of options like a normal person. :)

- **Options are now grouped**

  The old `{:mangle-js-names true}` is now `{:uglify-js {:mangle-names true}}`.
  In the same fashion, the new options for clean-css is bundled under `{:clean-css {...}}`

- CSS files with a single line over 5000 characters is considered already
  minified, and skipped. This avoid issues with huge bootstrap.css files
  and its ilk.

#### From 0.14 to 0.15

- Add cache-busting hash to end of path instead of beginning. (Francis Avila)

  This is so the root paths are still stable and predictable. For example, if a
  site keeps static files under `/static`, cache-busted files will still be
  under `/static`.

#### From 0.13 to 0.14

- Support for @imports in CSS
- Binary assets now has a `:resource` value instead of a `:get-stream` function
- Removed inline clj-v8 now that official repo supports bundling in uberjars
- Include both Cache-Control and Expires headers again, for Chrome's sake
- Improved documentation and bugfixes

#### From 0.12 to 0.13

- Add Last-Modified headers
- Remove Cache-Control headers (superflous when serving Expires)
- Create extension point for asset loading
- Bugfixes

#### From 0.11 to 0.12

- Move Angular.JS features into [its own project](http://github.com/magnars/optimus-angular).

#### From 0.10 to 0.11

- Add support for :base-path on assets for CDNs.
- Add exporting of assets to disk. Also for CDNs.

#### From 0.9 to 0.10

- Split strategies and optimizations so they can vary independently.

## Contribute

Yes, please do. And add tests for your feature or fix, or I'll
certainly break it later.

#### Installing dependencies

You need [npm](https://npmjs.org/) installed to fetch the JavaScript
dependencies. The actual fetching is automated however.

#### Running the tests

`lein midje` will run all tests.

`lein midje namespace.*` will run only tests beginning with "namespace.".

`lein midje :autotest` will run all the tests indefinitely. It sets up a
watcher on the code files. If they change, only the relevant tests will be
run again.

`lein with-profile +rhino midje [...]` will fetch and load Rhino dependencies, and execute
JS code in tests with it.

`lein with-profile +nashorn midje [...]` will use Nashorn to execute JS code in tests,
only on JDKs which ship with it.

## Contributors

- [Christian Johansen](https://github.com/cjohansen) added CSS and JS
  minification, and more.
- [Shaharz](https://github.com/shaharz) fixed a bug with external URLs
  in CSS-files.
- [Francis Avila](https://github.com/favila) improved placement of cache busters
  in URLs.
- [Anton Onyshchenko](https://github.com/env0der) added the `serve-live-assets-autorefresh` strategy.
- [Allen Rohner](https://github.com/arohner) fixed a memory leak.
- [Luke Snape](https://github.com/lsnape) fixed an issue with files referenced in CSS files when using `:base-url`.
- [radhika reddy](https://github.com/radhikalism) introduced JSR223, removing our dependency on V8 binaries.
- [August Lilleaas](https://github.com/augustl) added support for Windows.

Thanks!

## License

Copyright © Magnar Sveen, since 2013

Distributed under the Eclipse Public License, the same as Clojure.
