import React from 'react';
import PropTypes from 'prop-types';
import {withStyles} from '@material-ui/core/styles';
import Typography from '@material-ui/core/Typography';
import Grid from '@material-ui/core/Grid';
import Container from '@material-ui/core/Container';
import Divider from '@material-ui/core/Divider';
import Link from "@material-ui/core/Link";
import Copyright from "../component/Copyright";

const styles = (theme) => ({
  root: {
    marginTop: theme.spacing(6),
  },
  footer: {
    padding: theme.spacing(3, 0),
    [theme.breakpoints.up('sm')]: {
      padding: theme.spacing(8, 0),
    },
  },
  logo: {
    display: 'flex',
    alignItems: 'center',
    marginBottom: theme.spacing(4),
    '& img': {
      width: 28,
      height: 28
    },
  },
  list: {
    marginBottom: theme.spacing(4),
    '& h3': {
      fontWeight: theme.typography.fontWeightMedium,
    },
    '& ul': {
      margin: 0,
      padding: 0,
      listStyle: 'none',
    },
    '& li': {
      padding: '6px 0',
      color: theme.palette.text.secondary,
    },
  },
  version: {
    marginTop: theme.spacing(3),
  },
});

function BottomBar(props) {


  const { classes } = props;

  //let img = <img src="/logo.svg" alt="logo"/>

  return (
    <div className={classes.root}>
      <Divider />
      <Container maxWidth="md">
        <footer className={classes.footer}>
          <Grid container>
            <Grid item xs={6} sm={4} className={classes.list}>
              <Typography component="h2" gutterBottom>
                Community
              </Typography>
              <ul>
                <li>
                  <Link
                    color="inherit"
                    variant="body2"
                    href="https://github.com/mui-org/material-ui"
                  >
                    GitHub
                  </Link>
                </li>
                <li>
                  <Link color="inherit" variant="body2" href="https://twitter.com/MaterialUI">
                    Twitter
                  </Link>
                </li>
                <li>
                  <Link
                    color="inherit"
                    variant="body2"
                    href="https://stackoverflow.com/questions/tagged/material-ui"
                  >
                    StackOverflow
                  </Link>
                </li>
                <li>
                  <Link color="inherit" variant="body2" href="/discover-more/team/">
                    Team
                  </Link>
                </li>
              </ul>
            </Grid>
            <Grid item xs={6} sm={4} className={classes.list}>
              <Typography component="h2" gutterBottom>
                Resources
              </Typography>
              <ul>
                <li>
                  <Link color="inherit" variant="body2" href="/getting-started/support/">
                    Support
                  </Link>
                </li>
                <li>
                  <Link color="inherit" variant="body2" href="https://medium.com/material-ui/">
                    Blog
                  </Link>
                </li>
                <li>
                  <Link color="inherit" variant="body2" href="/components/material-icons/">
                    Component
                  </Link>
                </li>
              </ul>
            </Grid>
            <Grid item xs={6} sm={4} className={classes.list}>
              <Typography component="h2" gutterBottom>
                Company
              </Typography>
              <ul>
                <li>
                  <Link color="inherit" variant="body2" href="/company/about/">
                    About
                  </Link>
                </li>
                <li>
                  <Link color="inherit" variant="body2" href="/company/contact/">
                    Contact Us
                  </Link>
                </li>
              </ul>
            </Grid>
          </Grid>
          <Grid
            container
            direction="row"
            justify="center"
            alignItems="center"
          >
            <Copyright />
          </Grid>
        </footer>
      </Container>
    </div>
  );
}

BottomBar.propTypes = {
  classes: PropTypes.object.isRequired,
};

export default withStyles(styles)(BottomBar);
