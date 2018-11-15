package io.openshift.booster;

import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonObject;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

class Util{
    
    static void writeMessage(RoutingContext context, String message) {
        writeMessage(context, message, 200);
    }

    static void writeMessage(RoutingContext context, String message, int statusCode) {
        writeMessage(context, message, statusCode, null);
    }

    static void writeMessage(RoutingContext context, String message, int statusCode, JsonObject content) {
        JsonObject jsonObject = new JsonObject().put("message", message);
        if (content != null) {
            jsonObject.put("content", content);
        }
        context.response()
                .putHeader(CONTENT_TYPE, "application/json; charset=utf-8")
                .setStatusCode(statusCode)
                .end(jsonObject.encodePrettily());
    }
    
}