
dependencies {
  implementation(project(":bytle-fs"))
  implementation("org.jsoup:jsoup:1.13.1")
  implementation("net.sourceforge.cssparser:cssparser:0.9.29")
  // https://github.com/yui/yuicompressor - css javascript
  testImplementation(project(":bytle-http"))
}

description = "Web document wrapper and library"
