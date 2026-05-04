.DEFAULT_GOAL := all

BROWSERIFY_VERSION=10.2.6
#CLEAN_CSS_VERSION=5.3.3 -- not used, checking out directly from a PR branch for now
# we are waiting for this to get merged: https://github.com/clean-css/clean-css/pull/1275
UGLIFY_VERSION=2.4.24
BABEL_VERSION=7.20.13
CSSO_VERSION=5.0.5

RESOURCES_PATH=resources
UGLIFY_TARGET=$(RESOURCES_PATH)/uglify.js
CLEAN_CSS_TARGET=$(RESOURCES_PATH)/clean-css.js
CLEAN_CSS_PATH=node_modules/clean-css
CSSO_TARGET=$(RESOURCES_PATH)/csso.js
CSSO_PATH=node_modules/csso

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
	npm install github:bes-internal/clean-css#transition-behavior

$(CSSO_PATH):
	npm install csso@$(CSSO_VERSION)

$(UGLIFY_CMD):
	npm install uglify-js@$(UGLIFY_VERSION)

babel: $(RESOURCES_PATH)
	npm install @babel/standalone@$(BABEL_VERSION)
	cp node_modules/@babel/standalone/babel.min.js resources/babel.js

$(UGLIFY_TARGET): $(UGLIFY_CMD) $(RESOURCES_PATH)
	$(UGLIFY_CMD) --self -c -m -o resources/uglify.js

$(CLEAN_CSS_TARGET): $(BROWSERIFY_CMD) $(CLEAN_CSS_PATH) $(RESOURCES_PATH)
	$(BROWSERIFY_CMD)  -r clean-css -o resources/clean-css.js

$(CSSO_TARGET): $(CSSO_PATH) $(RESOURCES_PATH)
	cp node_modules/csso/dist/csso.js $(CSSO_TARGET)

all: $(UGLIFY_TARGET) $(CLEAN_CSS_TARGET) $(CSSO_TARGET) babel
