import React from "react";
import {Redirect, Route} from "react-router-dom";
import {useSelector} from "react-redux";


export function UserRoute({ children, ...rest }) {
  const name = useSelector(state => { return state.user.name })
  return (
    <Route
      {...rest}
      render={({ location }) =>
        name ? (
          children
        ) : (
          <Redirect
            to={{
              pathname: "/login",
              state: { from: location }
            }}
          />
        )
      }
    />
  );
}
