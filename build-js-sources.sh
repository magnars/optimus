#!/bin/sh

mkdir -p resources

if [ ! -d "node_modules/uglify-js" ]; then
    npm install uglify-js
fi

if [ ! -f "resources/uglify.js" ]; then
    ./node_modules/.bin/uglifyjs --self -c -m -o resources/uglify.js
fi

if [ ! -d "node_modules/browserify" ]; then
    npm install browserify
fi

if [ ! -d "node_modules/clean-css" ]; then
    npm install clean-css
fi

if [ ! -f "resources/clean-css.js" ]; then
    ./node_modules/.bin/browserify -r clean-css -o resources/clean-css.js
fi
