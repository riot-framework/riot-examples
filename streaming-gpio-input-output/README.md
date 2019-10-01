streaming-gpio-input-output
----------------

### Preparations
This example assumes the following has been done:
 - connect a green LED to GND/Pin 6 and GPIO07/Pin 7, with a resistor
 - connect a red LED to GND/Pin 6 and GPIO09/Pin 5, with a resistor
 - connect a push-button between GND/Pin 6 and GPIO15/Pin 8

The example runs fine without these steps, but there won't be anything visible besides the logging output. 

### Code walk-through

The main class, `Application`, creates a `Flow`, `Sink` and `Source` GPIO objects for pins 7, 9, and 5 on the Raspberry Pi.
Additionally, a standard Akka Timer is set up, which will emit a `GPIOState.TOGGLE` object every second. 
A `Sink` is define which logs everything it receives to the console.

We then define 2 streams:
- One in which the Timer is connected to GPIO 7: A green LED connected to this output now blinks.
- One in which GPIO 15 is connected to GPIO 9, but where the value is inverted in-between: When the button is press, GPIO 15 is connected to Ground, and becomes Low: GPIO 9 becomes high in response, and the red LED is lit.

Once the button is release, GPIO 15, which has been initialized with `withPullupResistor()` is 'pulled back' to the High state: it is connected to '+' with a resistor. GPIO 9 is set back to Low.
```java
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
``` 

### Running this example on a Raspberry Pi

RIoT comes with a tool (RIoT Control) which simplifies deployment to your Raspberry Pi. This tool is already preconfigured in this project. Set-up the name of your device and the user credentials to use for deployment in the build.sbt file:

```
riotTargets := Seq(
  riotTarget("raspberrypi", "pi", "raspberry")
)
```
Then connect your Device directly on a free port on your development machine, or on your local network. Then run <code>sbt riotRun</code> to compile your code, copy it to the device, and run it locally. 
The result will appear in your console. Press 'Enter' twice to stop the Program on your Pi. 