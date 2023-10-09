window.onload = () => {
  fetch("../openapi.yaml")
    .then((res) => res.text())
    .then((text) => {
      let openApiJson = jsyaml.load(text);
      window.ui = SwaggerUIBundle({
        //url: 'openapi.json', // a url can also be given
        spec: openApiJson,
        dom_id: '#swagger-ui',
      });
    })
    .catch((e) => console.error(e));

};
