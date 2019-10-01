import java.time.Duration;
import java.util.concurrent.CompletionStage;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import riot.GPIO;
import riot.GPIO.State;

/**
 * This is the typical 'Blink' example code using Akka Stream, and the simplest program you can create with RIoT. <br>
 * Connect a  LED to GND/Pin 6 and GPIO07/Pin 7, with a resistor). A timer is started that sends a 'Toggle' message
 * every half second. A Raspberry Pi GPIO pin receives this message, and toggles the green LED.
 */
public class Application {

    public static void main(String[] args) throws InterruptedException {
        ActorSystem system = ActorSystem.create("BlinkExample");
        Materializer mat = ActorMaterializer.create(system);

        // Create a 'Flow' element that controls GPIO pin 7:
        Flow<State, State, NotUsed> gpio7 = GPIO.out(7).initiallyLow().shuttingDownLow().asFlow(system);

        // Set up a timer: Send a GPIOState.TOGGLE object every 500 millis
        Source<GPIO.State, ?> timerSource = Source.tick(Duration.ZERO, Duration.ofMillis(500), GPIO.State.TOGGLE);

        // Also, we'll have a sink that logs the state returned by the GPIO flow:
        Sink<GPIO.State, CompletionStage<Done>> logSink = Sink
                .foreach(state -> System.out.println("GPIO 7 is now " + state));

        // On each timer tick, toggle the green LED and log the result.
        timerSource.via(gpio7).to(logSink).run(mat);
    }

}
