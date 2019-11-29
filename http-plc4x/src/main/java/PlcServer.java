import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.SystemConfiguration;
import org.apache.plc4x.java.opm.OPMException;
import org.apache.plc4x.java.opm.PlcEntityManager;
import org.apache.plc4x.java.utils.connectionpool.PooledPlcDriverManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * Example of using Akka HTTP to expose the input and output ports of a PLC. 2 paths will accept requests: <ul>
 * <li> /controlbox: Responds to GET requests only. A JSON representation of our device will be sent as a response.
 * </li>
 * <li> /status: Responds to POST requests containing a Status Lights object. Sets the corresponding outputs on our PLC
 * to high (42V) or low (0V) accordingly.</li>
 * </ul>
 */
public class PlcServer extends AllDirectives {

    // The PLC4X Entity Manager that will access our PLC and store its state in an annotated object
    PlcEntityManager entityManager = new PlcEntityManager(new PooledPlcDriverManager());

    // PLC4X-Specific connection URI:
    static final String connectionString = "s7://192.168.1.222/0/2?controller-type=S7_300";

    public static void main(String[] args) throws InterruptedException {
        ActorSystem system = ActorSystem.create("plc-server");
        Materializer mat = ActorMaterializer.create(system);
        Http http = Http.get(system);

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = new PlcServer().createRoute().flow(system, mat);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost("localhost", 8080), mat);

        Thread.currentThread().join();
    }

    /**
     * Sets up 2 routes: One to '/controlbox', which will respond to GET requests only. A JSON representation of our
     * device will be sent as a response. Another to 'status', whill will only respond to POST requests. The contents of
     * the payload will be decoded to a Status Lights object, and the corresponding outputs on our PLC will be set high
     * (42V) or low (0V) accordingly.
     *
     * @return an Akka HTTP Route object
     */
    private Route createRoute() {
        return concat(
                path("controlbox", () -> get(
                        () -> getControlBoxState())
                ),
                path("status", () -> post(
                        () -> entity(Jackson.unmarshaller(MyStatusLights.class), this::postStatusLights))
                )
        );
    }

    /**
     * Asynchronously fetches the value of the inputs mapped within the 'MyControlBox' class and returns them.
     *
     * @return an Akka HTTP Route object
     */
    private Route getControlBoxState() {
        CompletionStage<MyControlBox> future = CompletableFuture.supplyAsync(new Supplier<MyControlBox>() {
            @Override
            public MyControlBox get() {
                try {
                    return entityManager.read(MyControlBox.class, connectionString);
                } catch (OPMException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        return onSuccess(future, done ->
                completeOKWithFuture(future, Jackson.marshaller())
        );
    }

    /**
     * Asynchronously sets the value of the outputs mapped in the 'MyStatusLights' class, and returns them.
     *
     * @return an Akka HTTP Route object
     */
    private Route postStatusLights(MyStatusLights state) {
        CompletionStage<MyStatusLights> future = CompletableFuture.supplyAsync(new Supplier<MyStatusLights>() {
            @Override
            public MyStatusLights get() {
                try {
                    return entityManager.write(MyStatusLights.class, connectionString, state);
                } catch (OPMException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        return onSuccess(future, done ->
                completeOKWithFuture(future, Jackson.marshaller())
        );
    }

}
