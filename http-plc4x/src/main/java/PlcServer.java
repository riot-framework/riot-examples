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

import java.util.concurrent.CompletionStage;

/**
 * A skeleton for a RIoT application: Create a timer that sends a "Tick" message each second, then print it to the
 * console.
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

    private Route createRoute() {
        Configuration conf = new SystemConfiguration();
        conf.setProperty("org.apache.plc4x.java.opm.entity_manager.read_timeout", 2500);

        return concat(
                path("controlbox", () -> get(this::getControlBoxState)),
                path("status", () -> post(this::postStatusLights))
        );
    }

    private Route getControlBoxState() {
        try {
            final MyControlBox state = entityManager.read(MyControlBox.class, connectionString);
            return completeOK(state, Jackson.marshaller());
        } catch (OPMException e) {
            throw new RuntimeException(e);
        }
    }

    private Route postStatusLights() {
        try {

            return null;
        } catch (OPMException e) {
            throw new RuntimeException(e);
        }
    }

}
