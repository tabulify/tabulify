# Open API


The models are stated in openApi:
  * [Analytics](analytics-openapi.yaml)
  * [IP](ip-geolocation-openapi.yaml)

## How to generate the analytics event model.

You can generate the analytics model with gradle
```bash
cd vertx # this project
../gradlew openapi-analytics
```
Then you should make abstract:
  * the analyticsEvent class and its getName function
