import {CssBaseline} from "@material-ui/core";
import Helmet from "react-helmet";
import * as React from "react";
import Constants from "../Constant";

/**
 * To get meta dynamic from the server, see also
 * https://create-react-app.dev/docs/title-and-meta-tags
 *
 * @param string: title the page title
 * @return {*}
 */
export default function Head ({title = Constants.SiteTitle}) {
  // noinspection HtmlUnknownTarget
  return (
    <div>
      <CssBaseline />
      <Helmet>
        <title>{title} - {Constants.SiteName}</title>
        <meta charSet="utf-8"/>
        <link rel="icon" href="/favicon.ico"/>
      </Helmet>
    </div>
  )
}
