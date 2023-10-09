import Typography from "@material-ui/core/Typography";
import Link from "@material-ui/core/Link";
import React from "react";
import Constant from "../Constant"

export default function Copyright() {
  return (
    <Typography variant="body2" color="textSecondary" align="center">
      {'Copyright © '}
      <Link color="inherit" href={Constant.SiteUrl}>
        {Constant.SiteName}
      </Link>{' '}
      {new Date().getFullYear()}
      {'.'}
    </Typography>
  );
}
