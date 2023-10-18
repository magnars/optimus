# Old clean-css options

The new clean-css options are passed straight to clean-css, making available to
you all of the settings there. In the olden days (up until 2023.10.13), these
settings were available (here showing defaults):

```
:aggressive-merging true
:advanced-optimizations true
:keep-line-breaks false
:keep-special-comments "*"
:compatibility "*"
```

- `:aggressive-merging` - set to false to disable aggressive merging of properties.
- `:advanced-optimizations` - set to false to disable advanced optimizations; selector & property merging, reduction, etc.
- `:keep-line-breaks` - set to true to keep line breaks.
- `:keep-special-comments` - `"*"` for keeping all (default), `1` for keeping first one only, `0` for removing all
- `:compatibility` - enables compatibility mode, [see clean-css docs for examples](https://github.com/jakubpawlowicz/clean-css#how-to-set-compatibility-mode)

These old options will still work (we're trying not to break your stuff), but it
is probably a good idea to take a look at all the available settings in
clean-css.

## New clean-css options

Please see the [clean-css
documentation](https://github.com/clean-css/clean-css#constructor-options) for
available options. We now pass all options straight to clean-css.
