# Tabulify

Are you shipping data application with full confidence?

[Tabulify](https://www.tabulify.com) helps you ship data insights quickly with confidence.

## How

* Clean

```bash
tabul data drop *@database
tabul data truncate *@database
```

* Load or generate data

```bash
tabul data copy data_origin.csv @database
tabul data fill date@database
```

* Process

```bash
tabul data exec '(process.sql)@database'
tabul data exec process.sh
tabul data exec process.py
tabul data exec process.r
tabul data exec process.js
```

* Test

```bash
tabul data diff data_expected.csv data@database
```

* Deploy

```bash
tabul app deploy
```

## Quick Installation

On Windows WSL, Linux, macOS
* Installation
```bash
brew install tabulify/tap/tabulify
```
* Upgrade
```bash
brew update
brew upgrade tabulify/tap/tabulify
```
