#!/bin/sh

mkdir -p resources

if [ ! -d "node_modules" ]; then
  npm install
fi

if [ ! -f "resources/uglify.js" ]; then
    ./node_modules/.bin/uglifyjs --self -c -m -o resources/uglify.js
fi

