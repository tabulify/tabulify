/**
 * Example taken from
 * https://github.com/rt2zz/redux-persist#basic-usage
 * adapted with
 * https://redux.js.org/tutorials/fundamentals/part-6-async-logic#using-the-redux-thunk-middleware
 * and
 * to be able to see the store in DevTool
 * https://redux.js.org/recipes/configuring-your-store#integrating-the-devtools-extension
 */

import {applyMiddleware, createStore} from 'redux'
import thunkMiddleware from 'redux-thunk'
import {composeWithDevTools} from 'redux-devtools-extension'
import {persistReducer, persistStore} from 'redux-persist'
import storage from 'redux-persist/lib/storage' // defaults to localStorage for web
import rootReducer from './reducers'

const persistConfig = {
  key: 'root',
  storage,
}

const persistedReducer = persistReducer(persistConfig, rootReducer)

export default function exportedAnonymousFunction()  {
  let store = createStore(persistedReducer,composeWithDevTools(applyMiddleware(thunkMiddleware)))
  let persistor = persistStore(store)
  return { store, persistor }
};
