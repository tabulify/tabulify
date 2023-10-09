# Security


## Public

Public tools are HTML tool and should go through Cloudflare proxy.

## Internal

Internal tools are restricted:
  * by mandatory client certificate
  * by location
see [internal_tool](../templates/internal_tool.conf)

The tool that should be used on the desktop are not in the DNS and should be added
in a host file. Example: drill.bytle.net

They are not proxied because Cloudflare does not resend the client certificate.
