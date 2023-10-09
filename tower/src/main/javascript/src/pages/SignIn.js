import React from 'react';
import Avatar from '@material-ui/core/Avatar';
import Button from '@material-ui/core/Button';
import TextField from '@material-ui/core/TextField';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Checkbox from '@material-ui/core/Checkbox';
import Link from '@material-ui/core/Link';
import Grid from '@material-ui/core/Grid';
import LockOutlinedIcon from '@material-ui/icons/LockOutlined';
import Typography from '@material-ui/core/Typography';
import {makeStyles} from '@material-ui/core/styles';
import Container from '@material-ui/core/Container';
import {Link as RouterLink, useHistory, useLocation} from 'react-router-dom';
import Page from "../layout/Page";
import {useDispatch} from "react-redux";
import * as constants from "../redux/actionTypes";

/**
 * https://material-ui.com/styles/advanced/#overriding-styles-classes-prop
 * https://material-ui.com/styles/advanced/#makestyles-withstyles-styled
 */
const useStyles = makeStyles((theme) => ({
  paper: {
    marginTop: theme.spacing(5),
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
  },
  avatar: {
    margin: theme.spacing(1),
    backgroundColor: theme.palette.secondary.main,
  },
  form: {
    width: '100%', // Fix IE 11 issue.
    marginTop: theme.spacing(1),
  },
  submit: {
    margin: theme.spacing(3, 0, 2),
  },
}));

/**
 * https://react-redux.js.org/api/hooks
 * @return {*}
 * @constructor
 */
let SignIn = function () {
  let location = useLocation();
  let history = useHistory();
  const dispatch = useDispatch();


  /**
   * The origin page
   */
  let {from} = location.state || {from: {pathname: "/"}};

  let login = (event) => {
    event.preventDefault()

    dispatch({
      type: constants.USER_LOGGING_IN,
    })

    /**
     * Faking a network request
     * Wait 500ms seconds before "logging in"
     */
    setTimeout(() => {
      dispatch({
        type: constants.USER_LOGGED_IN,
        data: {
          name: "fake"
        }
      })
      /**
       * Go back
       */
      history.replace(from);
    }, 500)


  };

  const classes = useStyles();

  return (
    <Page title={"SignIn"}>
      <Container component="main" maxWidth="xs">
        <div className={classes.paper}>
          <Avatar className={classes.avatar}>
            <LockOutlinedIcon/>
          </Avatar>
          <Typography component="h1" variant="h5">
            Sign in Yolo From Yolo Nico
          </Typography>
          <p>You must log in in to view the `{from.pathname.substring(1)}` page.</p>
          <form className={classes.form} noValidate>
            <TextField
              variant="outlined"
              margin="normal"
              required
              fullWidth
              id="email"
              label="Email Address"
              name="email"
              autoComplete="email"
              autoFocus
            />
            <TextField
              variant="outlined"
              margin="normal"
              required
              fullWidth
              name="password"
              label="Password"
              type="password"
              id="password"
              autoComplete="current-password"
            />
            <FormControlLabel
              control={<Checkbox value="remember" color="primary"/>}
              label="Remember me"
            />
            <Button
              type="submit"
              fullWidth
              variant="contained"
              color="primary"
              className={classes.submit}
              onClick={login}
            >
              Sign In
            </Button>
            <Grid container>
              <Grid item xs>
                <Link component={RouterLink} to="/reset" variant="body2">
                  Forgot password?
                </Link>
              </Grid>
              <Grid item>
                <Link component={RouterLink} to="/register" variant="body2">
                  {"Don't have an account? Sign Up"}
                </Link>
              </Grid>
            </Grid>
          </form>
        </div>
      </Container>
    </Page>
  );
}

/**
 * https://react-redux.js.org/api/hooks#performance
 */
export default React.memo(SignIn)
