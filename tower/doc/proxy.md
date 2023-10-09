# Proxy

## About
When developing the [frontend app](app-frontend.md), the backend server
and the frontend server needs to communicate.


They do that with the help of a proxy

## Configuration

To develop an app, you can use the proxy configuration:
* of [the frontend Vite](#proxy-frontend-configuration), ie from the frontend server to the backend
* of [the backend Vertx](#proxy-backend-configuration). ie from the backend server to the frontend server

### Proxy Frontend Configuration

The document on the vite proxy can be found online [here](https://vitejs.dev/config/server-options.html#server-proxy).

With this configuration, you need to define all post api endpoint

### Proxy Backend Configuration

With this configuration, all `get` requests are forwarded to the port of the frontend server.

#### Backend
To enable the proxy, you need to set in the [configuration](env.md) [file](../.tower.yml),
the port:

Example for the [member app](member.md)
```env
combo.member.forward.proxy.port: 5173
```
All `get` requests will then be forwarded to the port `5173`

#### Vite Configuration

To work, the `vite application` should:
  * listen at minimum to the host name of the app
  * the hmr port should be defined to the server app

```javascript
// https://vitejs.dev/config/
// We need to define the HMR port as being the same as the
// development server because when we proxy the request
// through the java backend server (ie `member.nico.lan:8083`)
// the HMR port, is the serving port (ie `8083`)
// Vite will then reload each time trying to connect to the HMR server
// on a server that does not exist.
const port = 5175;
export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0', // listen to all connections, host name
    port: port,
    hmr: {
      port: port
    }
  }
})
```
