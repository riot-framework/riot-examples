import java.time.Duration;
import java.util.Arrays;
import java.util.List;
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
import riot.I2C;

public class Application {

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("riot-bma280-demo");
        Materializer mat = ActorMaterializer.create(system);

        // This sink will output the values returned straight to the console:
        Sink<BMA280.Results, CompletionStage<Done>> logSink = Sink.foreach(results -> System.out.println(results));

        // Assuming SDO pin is connected to GPIO 7: Setting it to LOW causes
        // BMA280.DEFAULT_ADDRESS to be used. Setting it to HIGH causes
        // BMA280.ALTERNATE_ADDRESS to be used. This allows several chips to be used on
        // one bus.
        GPIO.out(7).fixedAt(system, State.LOW);

        // Configure a BMA280 device on I2C bus 1
        BMA280 bma280config = new BMA280(
                BMA280Constants.AccelerometerScale.AFS_2G,
                BMA280Constants.Bandwidth.BW_500Hz,
                BMA280Constants.PowerMode.normal_Mode,
                BMA280Constants.SleepDuration.sleep100ms);

        Flow<BMA280.Command, BMA280.Results, NotUsed> bma280 = I2C
                .device(bma280config)
                .onBus(1)
                .at(BMA280Constants.DEFAULT_ADDRESS).asFlow(system);

        // Send a SELF-TEST, then a CALIBRATE command to it
        List<BMA280.Command> commands = Arrays.asList(BMA280.Command.SELFTEST, BMA280.Command.CALIBRATE);
        Source.from(commands).via(bma280).to(Sink.ignore()).run(mat);

        // Now, let's set up a timer: Send a READ command every 500 millis
        Source<BMA280.Command, ?> timerSource = Source.tick(Duration.ZERO, Duration.ofSeconds(1), BMA280.Command.READ);

        // Regularly query, then print out the values measured by the BMA280
        timerSource.via(bma280).to(logSink).run(mat);
    }

}
