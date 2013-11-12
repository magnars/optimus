#!/bin/sh

mkdir -p resources

if [ ! -d "node_modules/uglify-js" ]; then
    npm install uglify-js
fi

if [ ! -d "node_modules/csso" ]; then
    npm install csso
fi

if [ ! -f "resources/uglify.js" ]; then
    ./node_modules/.bin/uglifyjs --self -c -m -o resources/uglify.js
fi

if [ ! -f "resources/csso.js" ]; then
    cp ./node_modules/csso/web/csso.web.js resources/csso.js
fi
