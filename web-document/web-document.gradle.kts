val jsoupVersion = rootProject.ext.get("jsoupVersion").toString()
dependencies {
  // for the URI
  implementation(project(":bytle-type"))
  implementation(project(":bytle-fs"))
  implementation("org.jsoup:jsoup:$jsoupVersion")
  implementation("net.sourceforge.cssparser:cssparser:0.9.29")
  // https://github.com/yui/yuicompressor - css javascript
  testImplementation(project(":bytle-http"))
}

description = "Web document wrapper and library"
