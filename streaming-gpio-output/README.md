streaming-gpio-output
----------------

### Preparations
This example assumes you've connected a LED to GND/Pin 6 and GPIO07/Pin 7, with a 220 Ohm resistor in series.
The example runs fine without, but there won't be anything visible besides the logging output. 

### Code walk-through

The main class, `Application`, creates a `Flow` objects using RIoT which controls the output of the Raspberry Pi's General Purpose I/O port number 7, which is exposed on pin 7 of the Raspberry Pi's connector.
Additionally, a standard Akka Timer is set up, which will emit a `GPIOState.TOGGLE` object every second. 
A `Sink` is defined which logs everything it receives to the console.

We then define an Akka 'stream' which connects the Timer to our GPIO object, and then to the Sink: 
A green LED connected to this output would now blink, and the state of the port, returned by our GPIO Object every time we change its state, is output to the console.

```java
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
``` 

Running this example (see next section on how to do that) would produce an output similar to this:

```
...
[info] Done packaging.
[info] Opening session to raspberrypi.local
[info] Updated system clock
[info] Checking dependencies openjdk-8-jdk-headless wiringpi on raspberrypi.local
[info] Updating package list
[info] Reading package lists...
[info] Building dependency tree...
[info] Reading state information...
[info] openjdk-8-jdk-headless is already the newest version (8u212-b01-1+rpi1).
[info] wiringpi is already the newest version (2.50).
[info] 0 upgraded, 0 newly installed, 0 to remove and 60 not upgraded.
[info] Deploying streaming-gpio-output to raspberrypi.local
[info] To stop, press <Enter> twice.
[info] -- Logs begin at Tue 2019-10-01 09:21:06 BST. --
[info] Oct 01 21:56:48 raspberrypi systemd[1]: Started streaming-gpio-output.
[info] Oct 01 21:57:00 raspberrypi streaming-gpio-output[14918]: GPIO 7 is now HIGH
[info] Oct 01 21:57:01 raspberrypi streaming-gpio-output[14918]: GPIO 7 is now LOW
[info] Oct 01 21:57:01 raspberrypi streaming-gpio-output[14918]: GPIO 7 is now HIGH
[info] Oct 01 21:57:01 raspberrypi streaming-gpio-output[14918]: GPIO 7 is now LOW
[info] Oct 01 21:57:02 raspberrypi streaming-gpio-output[14918]: GPIO 7 is now HIGH
[info] Oct 01 21:57:02 raspberrypi streaming-gpio-output[14918]: GPIO 7 is now LOW
[info] Oct 01 21:57:03 raspberrypi streaming-gpio-output[14918]: GPIO 7 is now HIGH
[info] Oct 01 21:57:03 raspberrypi streaming-gpio-output[14918]: GPIO 7 is now LOW
[info] Oct 01 21:57:04 raspberrypi streaming-gpio-output[14918]: GPIO 7 is now HIGH
[info] Oct 01 21:57:04 raspberrypi streaming-gpio-output[14918]: GPIO 7 is now LOW
[info] Oct 01 21:57:05 raspberrypi streaming-gpio-output[14918]: GPIO 7 is now HIGH
[info] Oct 01 21:57:05 raspberrypi streaming-gpio-output[14918]: GPIO 7 is now LOW
[info] Oct 01 21:57:06 raspberrypi streaming-gpio-output[14918]: GPIO 7 is now HIGH
[info] Oct 01 21:57:06 raspberrypi streaming-gpio-output[14918]: GPIO 7 is now LOW


[info] Oct 01 21:57:07 raspberrypi streaming-gpio-output[14918]: GPIO 7 is now HIGH
[info] Oct 01 21:57:07 raspberrypi streaming-gpio-output[14918]: GPIO 7 is now LOW
[info] Closing session to raspberrypi.local

```

RIoT Control would connect to our Pi, then set up the system time, then ensure prerequisites for this project (Java 8 and the WiringPI library) are met.
Finally, it would copy the compiled code to the Pi, and start it. 

Pressing 'Enter' twice stops the program, and closes the connection to our Pi. Voilà, we've compiled, deployed and run our first RIoT application.

### Running this example on a Raspberry Pi

RIoT comes with a tool (RIoT Control) which simplifies deployment to your Raspberry Pi. This tool is already preconfigured in this project. Set-up the name of your device and the user credentials to use for deployment in the build.sbt file:

```
riotTargets := Seq(
  riotTarget("raspberrypi", "pi", "raspberry")
)
```
Then connect your Device directly on a free port on your development machine, or on your local network. Then run <code>sbt riotRun</code> to compile your code, copy it to the device, and run it locally. 
The result will appear in your console. Press 'Enter' twice to stop the Program on your Pi. 