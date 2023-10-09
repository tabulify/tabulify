# Realm (One realm, one domain)

## About
This page is about the `realm` object in the [data model](data_model.md)

A realm is a repository of users identity.
Upon successful identification, the email of the users is returned.

This is the top brand, the group name.

From a domain perspective, there is one realm by top-level domain
as it made it clear where the user is login in.

Key managers have one identity by origin.

## Cross-Domain pricing plan

In case, we want to support pricing plan across realms,
we may create a realm parent and add the user pricing plan up to the tree.
(ie the parent realm does not have any user but the permissions and role may be inherited)

If the user is logged with the same email on a child realm, it inherits the permissions.

## Google

For example google is the realm and has multiple apps on each subdomain.

https://myaccount.google.com/personal-info

where you get also profile picture, language information, gender

## Implementation
By default, the identity is the combostrap identity
but professional may want to have their own realm.

As for now, the identity repository is `combostrap`,
each user create a `combostrap.com` account,
and become a member of the realm (`app` or `apps`).
