package io.openshift.booster;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class HttpApplicationTest {

    private static final int PORT = 8081;

    private Vertx vertx;
    private WebClient client;
    private Logger logger;


    @Before
    public void before(TestContext context) {
        vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(new DropwizardMetricsOptions().setEnabled(true)));
        vertx.exceptionHandler(context.exceptionHandler());
        vertx.deployVerticle(HttpApplication.class.getName(),
                new DeploymentOptions().setConfig(new JsonObject().put("http.port", PORT)),
                context.asyncAssertSuccess());
        client = WebClient.create(vertx);
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @After
    public void after(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    //Todo: logging
    @Test
    public void testLogin(TestContext context) {
        logger.trace("Started login test.");

        Async async = context.async();
        client.get(PORT, "localhost", "/api/login?username=fabian&password=todo").send(resp -> {
            context.assertTrue(resp.succeeded());
            context.assertEquals(200, resp.result().statusCode());
            String message = resp.result().bodyAsJsonObject().getString("message");
            context.assertEquals(message, "ok");
            String cookie = resp.result().cookies().get(0);

            logger.info("Completed request to login endpoint. Message=\"" + message + "\" Cookie=\"" + cookie + "\"");
            client.get(PORT, "localhost", "/api/me").putHeader("Cookie", cookie.split(";")[0]).send(event -> {
                context.assertTrue(event.succeeded());
                context.assertEquals(200, event.result().statusCode());
                String message2 = event.result().bodyAsJsonObject().getString("message");
                context.assertEquals(message2, "ok");
                Integer id = event.result().bodyAsJsonObject().getJsonObject("content").getInteger("userid");
                context.assertEquals(1, id);

                logger.info("Completed request to me endpoint. Message=\"" + message2 + "\" Id=\"" + id + "\"");
                logger.info("Successfully completed login test!");
                async.complete();
            });
        });
    }

}
