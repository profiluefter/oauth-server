package io.openshift.booster;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

public class HttpApplication extends AbstractVerticle {

	static JDBCClient sql = null;

	@Override
	public void start(Future<Void> future) {
		// Create a router object.
		Router router = Router.router(vertx);

		router.route().handler(CookieHandler.create());
		router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

		router.get("/api/login").handler(UserAuthHandler::login);
		router.get("/api/logout").handler(UserAuthHandler::logout); router.get("/api/me").handler(UserAuthHandler::me);
		router.get("/*").handler(StaticHandler.create());

		sql = JDBCClient.createShared(vertx, new JsonObject().put("url", "jdbc:sqlite:database.sqlite3").put(
				"driver_class", "org.sqlite.JDBC").put("max_pool_size", 30));

		// Create the HTTP server and pass the "accept" method to the request handler.
		vertx.createHttpServer().requestHandler(router::accept).listen(
				// Retrieve the port from the configuration, default to 8080.
				config().getInteger("http.port", 8080), ar -> {
					if(ar.succeeded()) {
						System.out.println("Server started on port " + ar.result().actualPort());
					} future.handle(ar.mapEmpty());
				});
	}


}

