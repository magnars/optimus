.DEFAULT_GOAL := all

BROWSERIFY_VERSION=10.2.6
CLEAN_CSS_VERSION=3.0.7
UGLIFY_VERSION=2.4.24
BABEL_VERSION=7.20.13

RESOURCES_PATH=resources
UGLIFY_TARGET=$(RESOURCES_PATH)/uglify.js
CLEAN_CSS_TARGET=$(RESOURCES_PATH)/clean-css.js
CLEAN_CSS_PATH=node_modules/clean-css

ifeq ($(OS),Windows_NT)
  UGLIFY_CMD=node_modules\.bin\uglifyjs.cmd
  BROWSERIFY_CMD=node_modules\.bin\browserify.cmd
else
  UGLIFY_CMD=node_modules/.bin/uglifyjs
  BROWSERIFY_CMD=node_modules/.bin/browserify
endif

$(RESOURCES_PATH):
ifeq ($(OS),Windows_NT)
	if not exist $(RESOURCES_PATH) md $(RESOURCES_PATH)
else
	mkdir -p $(RESOURCES_PATH)
endif

$(BROWSERIFY_CMD):
	npm install browserify@$(BROWSERIFY_VERSION)

$(CLEAN_CSS_PATH):
	npm install clean-css@$(CLEAN_CSS_VERSION)

$(UGLIFY_CMD):
	npm install uglify-js@$(UGLIFY_VERSION)

babel: $(RESOURCES_PATH)
	npm install @babel/standalone@$(BABEL_VERSION)
	cp node_modules/@babel/standalone/babel.min.js resources/babel.js

$(UGLIFY_TARGET): $(UGLIFY_CMD) $(RESOURCES_PATH)
	$(UGLIFY_CMD) --self -c -m -o resources/uglify.js

$(CLEAN_CSS_TARGET): $(BROWSERIFY_CMD) $(CLEAN_CSS_PATH) $(RESOURCES_PATH)
	$(BROWSERIFY_CMD)  -r clean-css -o resources/clean-css.js

all: $(UGLIFY_TARGET) $(CLEAN_CSS_TARGET) babel
