package io.openshift.booster;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

public class HttpApplication extends AbstractVerticle {

    static final String template = "Hello, %s!";
    private JDBCClient sql;

    @Override
    public void start(Future<Void> future) {
        // Create a router object.
        Router router = Router.router(vertx);

        router.route().handler(CookieHandler.create());
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

        router.get("/api/login").handler(this::login);
        router.get("/api/logout").handler(this::logout);
        router.get("/api/me").handler(this::me);
        router.get("/*").handler(StaticHandler.create());


        // Create the HTTP server and pass the "accept" method to the request handler.
        vertx
                .createHttpServer()
                .requestHandler(router::accept)
                .listen(
                        // Retrieve the port from the configuration, default to 8080.
                        config().getInteger("http.port", 8080), ar -> {
                            if (ar.succeeded()) {
                                System.out.println("Server started on port " + ar.result().actualPort());
                            }
                            future.handle(ar.mapEmpty());
                        });
        sql = JDBCClient.createShared(vertx, new JsonObject().put("url", "jdbc:sqlite:database.sqlite3")
                .put("driver_class", "org.sqlite.JDBC")
                .put("max_pool_size", 30));
    }

    private void writeMessage(RoutingContext context, String message) {
        writeMessage(context, message, 200);
    }

    private void writeMessage(RoutingContext context, String message, int statusCode) {
        writeMessage(context, message, statusCode, null);
    }

    private void writeMessage(RoutingContext context, String message, int statusCode, JsonObject content) {
        JsonObject jsonObject = new JsonObject().put("message", message);
        if (content != null) {
            jsonObject.put("content", content);
        }
        context.response()
                .putHeader(CONTENT_TYPE, "application/json; charset=utf-8")
                .setStatusCode(statusCode)
                .end(jsonObject.encodePrettily());
    }

    private void me(RoutingContext rc) {
        Object userid = rc.session().get("userid");
        if (userid == null) {
            writeMessage(rc, "login-needed", 401);
        } else {
            writeMessage(rc, "ok", 200, new JsonObject().put("userid", userid));
        }
    }

    private void login(RoutingContext rc) {
        String username = rc.request().getParam("username");
        String password = rc.request().getParam("password");

        if (username == null || password == null) {
            writeMessage(rc, "username or password not specified", 401);
        } else if (username.equals("") || password.equals("")) {
            writeMessage(rc, "username or password can't be nothing");
        } else {
            sql.querySingleWithParams("SELECT id FROM users WHERE username=? AND password=?", new JsonArray().add(username).add(password), event -> {
                if (event.succeeded()) {
                    if (event.result() != null && event.result().getInteger(0) != null) {
                        rc.session().put("userid", event.result().getInteger(0));
                        writeMessage(rc, "ok");
                    } else if (event.result() == null) {
                        writeMessage(rc, "user-or-password-wrong", 401);
                    }
                } else {
                    event.cause().printStackTrace();
                    writeMessage(rc, "unknown-error", 500);
                }
            });
        }
    }

    private void logout(RoutingContext rc) {
        rc.session().remove("userid");
        writeMessage(rc, "ok");
    }
}
