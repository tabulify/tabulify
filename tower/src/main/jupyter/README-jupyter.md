# Jupyter Notebook

## Get started

Start a jupyter instance
```bash
docker run -p 10000:8888 --name jupyter -d quay.io/jupyter/datascience-notebook:2023-11-17
```

## Notebook
### Members
[members](members.ipynb) - an analysis of the mailchimp subscriber to see the bots (After 17 october 2019)

The resulting graphic are:
  * the [bots effect](members-bot-effect.json)
  * the [subscription by month](members-subscription-by-month.json)
That is a vega-lite json schema (that can runs in https://vega.github.io/editor/#/ to see the graphic)

The data is not stored as there are some emails.
