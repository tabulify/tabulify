import * as React from "react";
import Menu from "@material-ui/core/Menu";
import MenuItem from "@material-ui/core/MenuItem";
import IconButton from "@material-ui/core/IconButton";
import {AccountCircle} from "@material-ui/icons";
import {Link as RouterLink} from "react-router-dom";
import Link from "@material-ui/core/Link";
import {makeStyles} from "@material-ui/core/styles";
import {useDispatch, useSelector} from "react-redux";
import * as constants from "../redux/actionTypes";

const useStyles = makeStyles((theme) => ({
  login: {
    marginRight: theme.spacing(2),
  },
  join: {
    border: '1px solid',
    borderRadius: '6px',
    paddingTop: theme.spacing(1),
    paddingBottom: theme.spacing(1),
    paddingRight: theme.spacing(1),
    paddingLeft: theme.spacing(1),
  }
}));

export default function UserProfile() {
  const [anchorEl, setAnchorEl] = React.useState(null);
  const classes = useStyles();
  const name = useSelector(state => state.user.name)
  const dispatch = useDispatch();

  const handleMenu = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleLogout = () => {
    dispatch({
      type: constants.USER_LOGGED_OUT
    })
  }

  let loggedIn = (
    <div>
      <IconButton onClick={handleMenu} aria-label="account of current user" aria-controls="menu-appbar"
                  aria-haspopup="true" color="inherit"
      >
        <AccountCircle/>
      </IconButton>
      <Menu
        id="simple-menu"
        anchorEl={anchorEl}
        keepMounted
        open={Boolean(anchorEl)}
        onClose={handleClose}
      >
        <MenuItem onClick={handleClose}>My account</MenuItem>
        <MenuItem onClick={handleLogout}>Logout</MenuItem>
      </Menu>
    </div>
  );

  let loggedOut = (
    <div>
      <Link component={RouterLink} to="/login" variant="body1" color="inherit" className={classes.login}>
        Sign In
      </Link>

      <Link component={RouterLink} to="/join" variant="body1" color="inherit" className={classes.join}>
        Sign Up
      </Link>

    </div>
  )
  if (name) {
    return loggedIn;
  } else {
    return loggedOut;
  }
}
