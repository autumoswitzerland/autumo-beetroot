# Handlers

There are several handler types from which you can inherit.

## [CustomResponseHandler](https://github.com/autumoswitzerland/autumo-beetroot/blob/master/src/main/java/ch/autumo/beetroot/handler/CustomResponseHandler.java)

**Note**: Available in the coming release 3.0.1.

This handler can be used if a direct [HTTP response](https://github.com/autumoswitzerland/autumo-beetroot/blob/master/src/main/java/ch/autumo/beetroot/handler/HandlerResponse.java) without a HTML template should be delivered, e.g., when delivering a JSON output. This HTTP response is then 
attached to a [HandlerResponse](https://github.com/autumoswitzerland/autumo-beetroot/blob/master/src/main/java/ch/autumo/beetroot/handler/HandlerResponse.java)

**Example - `readData`-method answer**:

```Java
...
final Response liveSearchResponse = Response.newFixedLengthResponse(getStatus(), "application/json", jsonString);
return new HandlerResponse(HandlerResponse.STATE_OK, liveSearchResponse); 
```

## [NoConfigHandler](https://github.com/autumoswitzerland/autumo-beetroot/blob/master/src/main/java/ch/autumo/beetroot/handler/NoConfigHandler.java)

This handler can be used if there is no entity shown on a page and therefore has no `columns.cfg`. The current page is refreshed after this handler is executed.

## [NoContentAndConfigHandler](https://github.com/autumoswitzerland/autumo-beetroot/blob/master/src/main/java/ch/autumo/beetroot/handler/NoContentAndConfigHandler.java)

This handler can be used if no page is displayed at all. The current page is refreshed after this handler is executed. No `columns.cfg` is read.

## [NoContentHandler](https://github.com/autumoswitzerland/autumo-beetroot/blob/master/src/main/java/ch/autumo/beetroot/handler/NoContentHandler.java)

This handler can be used if no page is displayed at all. The current page is refreshed after this handler is executed.

## [NoContentButRouteHandler](https://github.com/autumoswitzerland/autumo-beetroot/blob/master/src/main/java/ch/autumo/beetroot/handler/NoContentButRouteHandler.java)

The same as the handler above, but instead refreshing the current page after this handler is executed, a specific given route is followed.


<br>
<br>
<a href="../README.md">[Main Page]</a>
