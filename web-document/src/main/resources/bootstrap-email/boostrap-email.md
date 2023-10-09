# Boostrap Email Css

## About
This is the bootstrap css email
of the [project](https://github.com/bootstrap-email/bootstrap-email.git)

We use it for the css email inliner

## How to compile it

```bash
yarn global add sass
git clone https://github.com/bootstrap-email/bootstrap-email.git bootstrap
cd bootstrap/core
sass bootstrap-email.scss > bootstrap-email.css
```

## Modification
Note that the margin-bottom for paragraph was changed from 0 to 16px.
[Discussion](https://github.com/bootstrap-email/bootstrap-email/discussions/206)

```css
p {
  text-align: left;
  line-height: 24px;
  font-size: 16px;
  margin-bottom: 16px;
}
```
