
description="A client that can be on a SPF server to deliver mail to inbox directly"

dependencies {
  /**
   * We pass the basic client
   */
  api(project(":bytle-smtp-client"))
  /**
   * We pass also DNS
   */
  api(project(":bytle-dns"))
}
