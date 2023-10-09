import * as actionTypes from '../actionTypes'

const userInitialState = {
  name: null,
  isLoading: false
}

/**
 *
 * @param state
 * @param action
 * @return {{isLoading: boolean, data: *}|{isLoading: boolean, data: null}}
 */
export default function user(state = userInitialState, action) {

  switch (action.type) {
    case actionTypes.USER_LOGGING_IN:
      return { ...userInitialState, isLoading: true }
    case actionTypes.USER_LOGGED_IN:
      return { name: action.data.name, isLoading: false }
    case actionTypes.USER_LOGGED_OUT:
      return userInitialState
    default:
      return state
  }
}
