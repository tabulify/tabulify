# Pagespeed


## About

The [PageSpeed Modules](https://www.modpagespeed.com/), mod_pagespeed (Apache) and ngx_pagespeed (Nginx),
are open-source webserver modules that optimize your site automatically.


They speed up your site and reduces page load time by automatically
applying web performance best practices to pages and associated assets (CSS,
JavaScript, images) without requiring you to modify your existing content or
workflow.

This is a copy of the README

## Installation / Build

There is pre-build binary for Apache, but there is none for Nginx.

The steps on [build
ngx_pagespeed from source](https://www.modpagespeed.com/doc/build_ngx_pagespeed_from_source)
have been added to this [tasks](../tasks/nginx_pagespeed.yml) and is called
[modules](../tasks/nginx_modules.yml)

## Features

Features include:

- Image optimization: stripping meta-data, dynamic resizing, recompression
- CSS & JavaScript minification, concatenation, inlining, and outlining
- Small resource inlining
- Deferring image and JavaScript loading
- HTML rewriting
- Cache lifetime extension
- and  [more](https://developers.google.com/speed/docs/mod_pagespeed/config_filters)

To see ngx_pagespeed in action, with example pages for each of the
optimizations, see our [demonstration site](http://ngxpagespeed.com)


## How to use

Follow the steps on [PageSpeed configuration](https://developers.google.com/speed/pagespeed/module/configuration)

For feedback, questions, and to follow the progress of the project:

- [ngx-pagespeed-discuss mailing
  list](https://groups.google.com/forum/#!forum/ngx-pagespeed-discuss)
- [ngx-pagespeed-announce mailing
  list](https://groups.google.com/forum/#!forum/ngx-pagespeed-announce)
