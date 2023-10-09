# React SSR

## About
This page shows a SSR integration with Vertx.

This is not yet done but gives a step by step on how to do it.

## Doc and example

  * [Vite documentation](https://vitejs.dev/guide/ssr.html#building-for-production)
  * [Vertx example based on Node](https://github.com/vert-x3/vertx-examples/blob/4.x/reactjs-server-side-rendering/README.md)

## Build
### Client

Client generation
```shell
yarn vite build --outDir dist/client --ssrManifest
```
```text
vite v4.3.9 building for production...
✓ 16 modules transformed.
dist/client/manifest.json              0.36 kB │ gzip: 0.17 kB
dist/client/ssr-manifest.json          0.52 kB │ gzip: 0.17 kB
dist/client/assets/react-35ef61ed.svg  4.13 kB │ gzip: 2.14 kB
dist/client/assets/App-e12e197a.css    0.48 kB │ gzip: 0.31 kB
dist/client/assets/App-ee1361eb.js     7.51 kB │ gzip: 2.80 kB
✓ built in 300ms
```

### Server

Server Apps.js generation
```shell
yarn vite build --outDir dist/server --ssr src/entry-server.js
```
```text
vite v4.3.9 building SSR bundle for production...
✓ 4 modules transformed.
dist/manifest.json      0.20 kB
dist/ssr-manifest.json  0.25 kB
dist/App.js             0.73 kB
✓ built in 158ms
```

## Java SSR

Then on the Server

Server rendering: The below code can be called from Java:
* to node
* with GraalJs
```javascript
import App from './App.js'
import React from 'react'
import ReactDOMServer from 'react-dom/server'
let output = ReactDOMServer.renderToStaticMarkup(React.createElement(App,{props}));
console.log(output)
```

The vertx handler should then serve this HTML template:
```html
<html>
<head>
  <script src="{{js}}"></script>
  <link rel="stylesheet" href="{{css}}"></link>
</head>
<body>
    <div id="root">
      {{output}}
    </div>
</body>
</html>
```
where:
* `js` = clientJs `assets/App-ee1361eb.js`
* `css` = clientCss `assets/App-e12e197a.css`
* `output` = from the SSR rendering
