import * as React from 'react'
import TopBar from './BarTop'
import {Container, makeStyles} from '@material-ui/core';
import Footer from './BarBottom';
import Constants from "../Constant";
import Head from "./Head";

const useStyles = makeStyles((theme) => ({
  pageContainer: {
    margin: theme.spacing(3)
  },
}));


/**
 * Any children that want to be in the standard page,
 * you can be a children
 * @param children
 * @param title
 * @return {*}
 */
export default function Page ({
  children,
  title = Constants.SiteTitle,
}){
  const classes = useStyles();
  return (
    <div>
      <Head title={title} />
      <Container maxWidth="md">
        <TopBar />
        <div className={classes.pageContainer}>
          {children}
        </div>
        <Footer />
      </Container>
    </div>
  )
}
