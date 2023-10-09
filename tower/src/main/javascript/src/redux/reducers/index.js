import {combineReducers} from 'redux'
import user from './user'

/**
 * https://redux.js.org/api/combinereducers
 * @type {Reducer<CombinedState<unknown>>}
 */
export default combineReducers({
  user
})
