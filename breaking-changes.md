## Breaking changes

### 2022-02-13

- **We have removed clj-v8, and replaced it with pluggable JS-engine JSR223**

  This lets us support Windows, and avoid shipping binaries. For most people,
  this should not be a breaking change. Do let us know if anything broke for
  you. Some Optimus middlewares might need to be upgraded to the newest version.

### 0.19.0

- **We have updated to Clojure 1.8.0 and now require JDK 1.7 or higher.**

  This let us fix loading issues with `serve-live-assets-autorefresh`.

### 0.17.0

- **The optimus.hiccup namespace is removed in favor of optimus.html**

  The old `optimus.hiccup/link-to-css-bundles` created hiccup data structures,
  which is a mistake. It's specific to one rendering method, and Hiccup works
  just as well (or better) with strings.

  If you were using `optimus.hiccup`, just replace it will `optimus.html` and
  all is well.

### 0.16.0

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
