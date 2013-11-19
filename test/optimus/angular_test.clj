(ns optimus.angular-test
  (:use [optimus.angular]
        [optimus.test-helper]
        [midje.sweet]))

(fact
 "Angular.JS fetches your templates one at a time, resulting in pretty
  horrid performance while it's getting all the html to render your
  page. Optimus lets you serve your templates up front."

 (with-files [["/templates/simple.html" "Hi! \\ Your name is \"{{ name }}\".\n"]]

   (create-template-cache
    :path "/my-templates.js"
    :module "myapp"
    :public-dir public-dir
    :templates ["/templates/simple.html"])

   => {:path "/my-templates.js"
       :bundle nil
       :contents "angular.module(\"myapp\").run([\"$templateCache\", function ($templateCache) {\n  $templateCache.put(\"/templates/simple.html\", \"Hi! \\\\ Your name is \\\"{{ name }}\\\".\\n\");\n}]);"}))

(fact
 "Not only can you serve all your templates as one file, you can
  bundle them together with one of your javascript bundles."

 (with-files [["/templates/multiple/one.html" "<h1>One</h1>\n\n<p>I am one.</p>\n\n<script>\n  // Test\n  /* comments */\n  var foo = 'bar';\n</script>\n"]
              ["/templates/multiple/two/two.html" "<h2>Two</h2>\n\n<p>We are two.</p>\n"]]

   (create-template-cache
    :path "/multiple-templates.js"
    :bundle "app.js"
    :module "multiple"
    :public-dir public-dir
    :templates ["/templates/multiple/one.html"
                "/templates/multiple/two/two.html"])

   => {:path "/multiple-templates.js"
       :bundle "app.js"
       :contents "angular.module(\"multiple\").run([\"$templateCache\", function ($templateCache) {\n  $templateCache.put(\"/templates/multiple/one.html\", \"<h1>One</h1>\\n\\n<p>I am one.</p>\\n\\n<script>\\n  // Test\\n  /* comments */\\n  var foo = 'bar';\\n</script>\\n\");\n  $templateCache.put(\"/templates/multiple/two/two.html\", \"<h2>Two</h2>\\n\\n<p>We are two.</p>\\n\");\n}]);"}))
