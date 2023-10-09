# Frontend app (HTML assets)

## About
A [frontend app](app-frontend.md) is an [app](app.md) that targets the browser
as environment.

It:
* produces and creates HTML and its resources
* does have the notion of a session
  and have therefore [extra security](../src/main/java/net/bytle/tower/util/BrowserSecurityUtil.java)


## Location

All HTML resources are located:
* in the [resources webroot directory](../src/main/resources/webroot).
* by app

Example:
* for the [combo/member app](app.md), the resources are located in [webroot/combo/member](../src/main/resources/webroot/combo/member)

Why?
* Because it allows to refer in the src to the script and stylesheet resources and have the IDE not complaining.
* Follows the build principle of Javascript builder

## HTML template page

HTML pages are developed and served with the thymeleaf template engine.

They are compiled and serve using a ComboApp object.

Example for the ComboPublicApi
````java
String formHtml = CombApiPublic
          .get()
          .getTemplate("list-registration-form.html")
          .applyVariables(variables)
          .getResult();
````


## Static Resources

Static resources such as javascript, css, image, ... are also served from the [static resources directory](../src/main/resources/webroot)
to allow navigation from code editor.

They are restricted to the `assets` directory that is also used as path for the rerouting.

Example:
* for the [combo/member app](app.md), the resources are located in [webroot/combo/member/assets](../src/main/resources/webroot/combo/member/assets)

## Javascript
While the HTML are templates, the javascript files are not, they are [static resources](#static-resources)
It means that if Javascript needs a variable, it should be set in the HTML page via
* script
* or the meta html tag

Example:
```html
<script>
    let csListRegistrationEndpoint = "[[${endpoint}]]";
</script>
```

## Development Server

You can develop:
* with a single HTML page (reloading manually with F5 without build step)
* or with the help of a node dev server. If this is the case, you can [proxy the get requests](proxy.md)
