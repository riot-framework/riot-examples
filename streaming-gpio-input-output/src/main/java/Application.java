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
 * Example code using Akka Stream that reacts to a button push by making one LED light up. Meanwhile, another LED will blink
 * continually:
 * <li>connect a green LED to GND/Pin 6 and GPIO07/Pin 7, with a resistor) <br>
 * <li>connect a red LED to GND/Pin 6 and GPIO09/Pin 5 <br>
 * <li>connect a push-button between GND/Pin 6 and GPIO15/Pin 8 <br>
 * A timer is started that sends a 'Toggle' message every half second. A Raspberry Pi GPIO pin receives this message,
 * and toggles the green LED. When the push-button is pressed, the GPIO 15 is shorted. This is detected by the program,
 * and the red LED is lit in response.
 */
public class Application {

    public static void main(String[] args) throws InterruptedException {
        ActorSystem system = ActorSystem.create("BlinkExample");
        Materializer mat = ActorMaterializer.create(system);

        // You can easily make Flows, Sources, Sinks or Actors out of GPIO Pins:
        Flow<State, State, NotUsed> gpio7 = GPIO.out(7).initiallyLow().shuttingDownLow().asFlow(system);
        Sink<GPIO.State, NotUsed> gpio9 = GPIO.out(9).initiallyLow().shuttingDownLow().asSink(system);
        Source<State, NotUsed> gpio15 = GPIO.in(15).withPullupResistor().asSource(system, mat);
        // or just plain actors: system.actorOf(GPIO.out(8).asProps());

        // Now, let's set up a timer: Send a GPIOState.TOGGLE object every 500 millis
        Source<GPIO.State, ?> timerSource = Source.tick(Duration.ZERO, Duration.ofMillis(500), GPIO.State.TOGGLE);

        // Also, we'll have a sink that logs the state returned by the GPIO flow:
        Sink<GPIO.State, CompletionStage<Done>> logSink = Sink
                .foreach(state -> System.out.println("GPIO 7 is now " + state));

        // Let's define 2 streams:
        // - On each timer tick, toggle the green LED and log the result.
        // - When GPIO 15 becomes Low (button is pressed), switch on the red LED.
        timerSource.via(gpio7).to(logSink).run(mat);
        gpio15.map(state -> state == State.LOW ? State.HIGH : State.LOW).to(gpio9).run(mat);

        // Wait forever
        Thread.currentThread().join();
    }

}
