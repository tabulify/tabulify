# Analytics User identification


Parts of [Analytics](analytics.md)

## Identity Call and identification

When you make an identify call, the analytics library saves:
* in the browser
  * the userId to the browser cookie and localstorage,
  * and writes all the user traits in localStorage.
* in a native app:
  * the userId and traits are stored in the device’s memory.

This makes it possible to append the user’s data to all subsequent page calls or track calls for the user.

If a user returns to your site after the cookie expires, the library looks for an old ID in the user’s localStorage, and if one is found, sets it as the user’s ID again in a new cookie.

If the user clears their cookies and localStorage, all of the IDs are removed and the user gets a completely new anonymousId when they next visit the page.

Whenever possible, an identify call should be followed with a track event
that records what caused the user to be identified.

## Cookie

The ID cookie is set with a one-year expiration.

## Anonymous ID
The key is to have an anonymous ID (uuid.uuid4())
that is unique to each user and persists during that user's session.

We recommend generating a UUID and storing that value in a cookie.

Mixpanel's SDKs will generate a `$device_id` to associate these events to the same anonymous user.

It's called a device id because it's generated and unique for each device.
See https://docs.mixpanel.com/docs/tracking/how-tos/identifying-users#example-user-flows

## Distinct ID

Distinct_id is an identifier based on the combination of
  * $device_id (uuid v4)
  * and $user_id.
The purpose of distinct_id is to provide a single, unified identifier for a user across devices and sessions.

It helps compute metrics like Daily Active Users accurately: when two events have the same value of distinct_id, they are considered as being performed by 1 unique user.

## When ?

You should make an identify call in the following situations:
* When the user provides any identifying information (such as a newsletter sign-up with email and name)
* When first you create a user (and so it is assigned a userId)
* When a user changes information in their profile
* When a user logs in
* (Optional) Upon loading any pages that are accessible by a logged-in user

## Ref

https://segment.com/docs/connections/spec/best-practices-identify/
