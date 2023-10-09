

dependencies {
    api(project(":bytle-db"))
    api("org.apache.poi:poi:3.17")
    api("org.apache.poi:poi-ooxml:3.17")
    api("org.apache.commons:commons-collections4:4.1") //<!-- Needed by XSSFWorkbook of org.apache.poi.xssf (not in the dependencies) -->
}

description = "Excel"
