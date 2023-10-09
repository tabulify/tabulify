import React from 'react'
import UserProfile from "../component/UserProfile";
import Toolbar from "@material-ui/core/Toolbar";
import AppBar from "@material-ui/core/AppBar";
import makeStyles from "@material-ui/core/styles/makeStyles";
import Link from "@material-ui/core/Link";
import {Link as RouterLink} from "react-router-dom";
import Constant from "../Constant";

// Doc
// https://material-ui.com/components/app-bar/
// https://material.io/components/app-bars-top
// https://material.io/develop/web/components/top-app-bar/
// https://material-ui.com/api/toolbar/
//  is triggered by tapping the hamburger menu icon.
// https://material.io/design/components/navigation-drawer.html
// https://material.io/components/navigation-rail
// https://medium.com/@habibmahbub/create-appbar-material-ui-responsive-like-bootstrap-1a65e8286d6f

// Discontinuated
// https://github.com/material-components/material-components-web-react
// https://material-components.github.io/material-components-web-catalog/

// Wrapper for
// https://github.com/material-components/material-components-web

//
// https://material-ui.com/components/use-media-query/

const useStyles = makeStyles((theme) => ({
  pushRight: {
    display: 'flex',
    flexGrow: 1,
  },
  barElement: {
    display: 'flex',
    marginRight: theme.spacing(1),
  },
}));

export default function TopBar() {
  const classes = useStyles();
  let img = <img src="/logo.svg" alt="logo"/>
  return (
    <AppBar position="static" color={"transparent"} elevation={0}>
      <Toolbar>
        <div className={classes.pushRight}>
          <Link component={RouterLink} to="/" variant="body1" color="inherit" className={classes.barElement}>
            {img}
          </Link>
          <Link component={RouterLink} to="/" variant="body1" color="inherit" className={classes.barElement}>
            {Constant.SiteName}
          </Link>
        </div>
        <UserProfile/>
      </Toolbar>
    </AppBar>
  )
}
