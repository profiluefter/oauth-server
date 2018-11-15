package io.openshift.booster;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import static io.openshift.booster.Util.*;

class UserAuthHandler {
    static void me(RoutingContext rc) {
        Object userid = rc.session().get("userid");
        if (userid == null) {
            writeMessage(rc, "login-needed", 401);
        } else {
            writeMessage(rc, "ok", 200, new JsonObject().put("userid", userid));
        }
    }

    static void login(RoutingContext rc) {
        String username = rc.request().getParam("username");
        String password = rc.request().getParam("password");

        if (username == null || password == null) {
            writeMessage(rc, "username or password not specified", 401);
        } else if (username.equals("") || password.equals("")) {
            writeMessage(rc, "username or password can't be nothing");
        } else {
            HttpApplication.sql.querySingleWithParams("SELECT id FROM users WHERE username=? AND password=?", new JsonArray().add(username).add(password),
                                      event -> {
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

    static void logout(RoutingContext rc) {
        rc.session().remove("userid");
        writeMessage(rc, "ok");
    }
}
