import React from "react";
import MainLayout from "../../layout/Page";
import Button from "@material-ui/core/Button";
import {ReactComponent as Windows} from "./windows.svg"
import {ReactComponent as Linux} from './linux.svg';
import makeStyles from "@material-ui/core/styles/makeStyles";
import Grid from "@material-ui/core/Grid";
import Container from "@material-ui/core/Container";
import Typography from "@material-ui/core/Typography";

const useStyles = makeStyles((theme) => ({
  paper: {
    marginTop: theme.spacing(4),
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center'
  },
  imageIcon: {
    height: '48px'
  }
}));


/**
 *
 * @return {*}
 * @constructor
 *
 * File origin:
 * Windows File: https://commons.wikimedia.org/wiki/File:Windows_logo_-_2012.svg
 * Linux File: https://commons.wikimedia.org/wiki/File:Linux_Logo_in_Linux_Libertine_Font.svg
 *
 * Method:
 * https://create-react-app.dev/docs/adding-images-fonts-and-files/#adding-svgs
 */
export default function Download() {


  const classes = useStyles();


  function downloadReport() {
    alert("Download")
  }

  return (

    <MainLayout title={"Download"}>
      <Container component="main" maxWidth="sm">
        <div className={classes.paper}>
          <Typography component="h1" variant="h5">
            Download
          </Typography>
          <Grid container className={classes.root} direction="row" justify="center" alignItems="center" spacing={2}>
            <Grid item xs={6}>
              <Grid container direction="column" justify="center" alignItems="center" spacing={2}>
                <Grid item>
                  <Linux className={classes.imageIcon}/>
                </Grid>
                <Grid item>
                  <Button size={'small'} variant="contained" onClick={downloadReport}>Linux</Button>
                </Grid>
              </Grid>
            </Grid>
            <Grid item xs={6}>
              <Grid container direction="column" justify="center" alignItems="center" spacing={2}>
                <Grid item>
                  <Windows className={classes.imageIcon}/>
                </Grid>
                <Grid item>
                  <Button size={'small'} variant="contained" onClick={downloadReport}>Windows</Button>
                </Grid>
              </Grid>
            </Grid>
          </Grid>
        </div>
      </Container>
    </MainLayout>

  )
}
