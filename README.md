# warcmock
Proxy for serving WARC files.

Deploy the web application archive (war) file in target/ as
the ROOT application of your servlet container (e.g., Apache Tomcat).

You can configure the location of the WARC file in ./src/main/webapp/WEB-INF/web.xml:

```xml
<context-param>
   <param-name>warcfile</param-name>
   <param-value>/tmp/archive.warc.gz</param-value>
</context-param>
```

Then, configure your HTTP user agent to use as proxy the hostname
and port on which the servlet container runs (e.g., http://localhost:8080/).

    export http_proxy="http://localhost:8080/"