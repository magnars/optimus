# optimus

A Ring middleware for frontend performance optimization.

## How to run the tests

`lein midje` will run all tests.

`lein midje namespace.*` will run only tests beginning with "namespace.".

`lein midje :autotest` will run all the tests indefinitely. It sets up a
watcher on the code files. If they change, only the relevant tests will be
run again.

## Design thoughts

The most basic operation of optimus is serving files from a list, with
this basic structure:

    [{:path :contents}]

It serves the `:contents` if `:uri` in the request matches `:path`.

Built on top of that is a bunch of operations that either help you:

 - add files to the list
 - optimize the files in the list somehow
 - link to the files

On top of the basics of `:path` and `:contents`, the file map may contain:

 - `:bundle` - the name of the bundle this file is part of.
 - `:headers` - headers to be served along with the file.
 - `:original-path` - the path before any changes was made, like cache-busters.
 - `:outdated` - will only be served when referenced directly.
 - `:browsers` - will only be served to this set of browsers

Let's look at some examples.

### Adding files to the list

You can certainly build the list of files manually, but there are
several ways in which optimus helpes you create the list:

 - given a path on the class path, it can read in the contents, making
   sure the file exists.

 - create a bunch of files and assign them to a bundle in one operation

 - find files referenced in the files you're adding (note: the
   porcelain described earlier does this for you).

It's all sugar, but it's tasty.

### Optimizing the files

After creating the initial list of files, you might want to tune it.
For instance:

 - bundle together files to avoid too many HTTP requests
 - minimize JavaScript and CSS.
 - add far-future expires headers and change the path to include cache busters

### Link to the files

After you've changed the path to a file, or bundled a bunch together,
you need a way of referencing them in your HTML. Optimus lets you link
to files by their original path. Or link to a bunch of files by their
bundle name.

You don't have to serve all files through optimus. Any paths that are
not present in the list of files will be passed to the next ring
middleware. So you can serve additional static resources from
`compojure.route/resources` for instance.
