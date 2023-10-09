import SignIn from "./pages/SignIn";
import SignUp from "./pages/SignUp";
import {BrowserRouter as Router, Route, Switch} from "react-router-dom";
import React from "react";
import Download from "./pages/download/Download";
import PasswordReset from "./pages/PasswordReset";
import {UserRoute} from "./routing/UserRoute";


/**
 * Rendering
 * @return {*}
 * @constructor
 */
function App() {
  return (
      <Router>
        <Switch>
          <Route path="/login">
            <SignIn/>
          </Route>
          <Route path="/register">
            <SignUp/>
          </Route>
          <UserRoute path="/download">
            <Download/>
          </UserRoute>
          <Route path="/reset">
            <PasswordReset/>
          </Route>
          <Route path="/">
            <SignIn/>
          </Route>
        </Switch>
      </Router>
  );
}

export default App;
