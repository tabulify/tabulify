# HTML, Javascript and CSS document manipulation (Web File)

## About

This module is all about the manipulation of web document file (HTML)

TODO: Move in the type module with json and yaml ?

## Dependencies


### Select (Parsing)

  * https://jsoup.org/


### CSS

We use [CSSParser](http://cssparser.sourceforge.net/gettingStarted.html)
because it's implementing and given in the [SAC](https://www.w3.org/Style/CSS/SAC/) page


Others. See also the [SAC](https://www.w3.org/Style/CSS/SAC/)
  * https://github.com/radkovo/jStyleParser
  * https://github.com/phax/ph-css

### Browser
GUI-Less browser
https://htmlunit.sourceforge.io/

### Note

Just some information.

#### Minification

Minification is the process of removing all unnecessary characters
from the source codes of interpreted programming languages or markup languages
without changing their functionality

  * The YUI Compressor is written in Java (requires Java >= 1.4) and relies on Rhino to tokenize the source JavaScript file.
  * [Compiler - JavaScript optimizer](https://developers.google.com/closure/compiler). It compiles from JavaScript to better JavaScript. It parses your JavaScript, analyzes it, removes dead code and rewrites and minimizes what's left. It also checks syntax, variable references, and types, and warns about common JavaScript pitfalls
  * [Web Resource Optimizer for Java](https://github.com/wro4j/wro4j)
  * [HtmlCleaner](http://htmlcleaner.sourceforge.net/index.php) from HTML to well transformed XML


  * [String compression (Gzip)](https://stackoverflow.com/questions/16351668/compression-and-decompression-of-string-data-in-java/16351783#16351783)

#### WebJars

  * https://www.webjars.org/documentation#vertx

Example:
```java
Router router = Router.router(vertx);
router.route("/assets/lib/*").handler(StaticHandler.create("META-INF/resources/webjars"));
```
```html
<link rel='stylesheet' href='assets/lib/bootstrap/3.1.0/css/bootstrap.min.css'>
```

#### Rendering

  * https://github.com/radkovo/CSSBox


#### See also

  * [Url Watch](https://github.com/thp/urlwatch/blob/master/README.md)
