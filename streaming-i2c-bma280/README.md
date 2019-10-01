streaming-i2c-bma280
----------------

### Preparations
The [BMA280] is a high resolution, compact, and reasonably-priced accelerometer.
This example assumes you've connected yours to your Raspberry Pi as follows:
- VCC to Pin 1 (3.3v)
- GND to Pin 9 (GND)
- SCL to Pin 3 (SCL1)
- SDA to Pin 3 (SDA1)
- SDO to Pin 7 (GPIO7), or to GND, or unconnected 
- INT1, INT2, and CSB unconnected

### Code walk-through
A RIoT GPIO object is created with a fixed value: Low. This is connected to SDO, which controls the BMA280's addess: 
Changing it to high will make the program fail, until the address used to communicate with the BMA280 is changed accordingly.

The `BMA280` class encapsulates the chip's protocol, including which commands can be issues by us, and how it is configured.
A BMA280 object is instantiated with our settings. 
A `Flow` objects is then set up with the BMA280 instance, which will immediately initialize the chip.

Two sources are then set up:

- One is defined based on a list of commands: SELF-TEST, then CALIBRATE (These commands are defined in the BMA280 class)
- Additionally, an Akka Timer source is set up, which will emit a `READ` command every second. 

One flow is defined and started first: From the commands source, to the I2C device. The output is ignored (it's directed to `Sink.ignore()`, which does nothing).

Execution continues only after all messages in the list of commands have been consumed.

The second flow then goes from the Timer, to the I2C device, to the logging sink: Every second, a READ command is issued, then received by
the device, which reacts to it by reading the values from the accelerometer, and replies with a value object containing the measurements. This object is
then logged to the console.

```java
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
``` 

Running this example would produce an output similar to this:

```
...
[info] Opening session to raspberrypi.local
[info] Updated system clock
[info] Ensuring features are enabled on raspberrypi.local: I2C
[info] Dependencies unchanged since last install, skipping check on raspberrypi.local
[info] Deploying streaming-i2c-bma280 to raspberrypi.local
[info] To stop, press <Enter> twice.
[info] -- Logs begin at Tue 2019-10-01 10:09:10 BST. --
[info] Oct 01 22:21:33 raspberrypi systemd[1]: Started streaming-i2c-bma280.
[info] Oct 01 22:21:45 raspberrypi streaming-i2c-bma280[15318]: INFO BMA280 - Initialized. Chip ID 0xfb.
[info] Oct 01 22:21:46 raspberrypi streaming-i2c-bma280[15318]: INFO BMA280 - Starting self-test
[info] Oct 01 22:21:46 raspberrypi streaming-i2c-bma280[15318]: INFO BMA280 - X-axis self test = 3999.5117 mg, should be > 800 mg
[info] Oct 01 22:21:46 raspberrypi streaming-i2c-bma280[15318]: INFO BMA280 - Y-axis self test = 3999.5117 mg, should be > 800 mg
[info] Oct 01 22:21:46 raspberrypi streaming-i2c-bma280[15318]: INFO BMA280 - Z-axis self test = 3999.5117 mg, should be > 400 mg
[info] Oct 01 22:21:46 raspberrypi streaming-i2c-bma280[15318]: INFO BMA280 - Calibration complete. Offsets: X=0.0mg, Y=0.0mg, Z=0.0mg
[info] Oct 01 22:21:46 raspberrypi streaming-i2c-bma280[15318]: BMA280.Results [x=-0.0034g, y= 0.0137g, z=-0.0474g, temp=22.50°C]
[info] Oct 01 22:21:47 raspberrypi streaming-i2c-bma280[15318]: BMA280.Results [x=-0.0173g, y= 0.9439g, z=-0.0112g, temp=22.50°C]
[info] Oct 01 22:21:48 raspberrypi streaming-i2c-bma280[15318]: BMA280.Results [x=-0.0197g, y= 0.9300g, z=-0.0226g, temp=22.50°C]
[info] Oct 01 22:21:49 raspberrypi streaming-i2c-bma280[15318]: BMA280.Results [x= 0.0005g, y= 0.9307g, z=-0.0148g, temp=22.50°C]
[info] Oct 01 22:21:50 raspberrypi streaming-i2c-bma280[15318]: BMA280.Results [x= 0.0023g, y= 0.9332g, z=-0.0136g, temp=22.50°C]
[info] Oct 01 22:21:51 raspberrypi streaming-i2c-bma280[15318]: BMA280.Results [x= 0.0013g, y= 0.9432g, z=-0.0156g, temp=22.50°C]
[info] Oct 01 22:21:52 raspberrypi streaming-i2c-bma280[15318]: BMA280.Results [x= 0.0025g, y= 0.9302g, z=-0.0114g, temp=22.50°C]
[info] Oct 01 22:21:53 raspberrypi streaming-i2c-bma280[15318]: BMA280.Results [x=-0.0190g, y= 0.9344g, z=-0.0231g, temp=22.50°C]
[info] Oct 01 22:21:54 raspberrypi streaming-i2c-bma280[15318]: BMA280.Results [x= 0.0037g, y= 0.9449g, z=-0.0095g, temp=22.50°C]
[info] Oct 01 22:21:55 raspberrypi streaming-i2c-bma280[15318]: BMA280.Results [x=-0.0168g, y= 0.9446g, z=-0.0124g, temp=22.50°C]
[info] Oct 01 22:21:56 raspberrypi streaming-i2c-bma280[15318]: BMA280.Results [x= 0.0018g, y= 0.9307g, z=-0.0109g, temp=22.50°C]
[info] Oct 01 22:21:57 raspberrypi streaming-i2c-bma280[15318]: BMA280.Results [x= 0.0008g, y= 0.9427g, z=-0.0148g, temp=22.50°C]
[info] Oct 01 22:21:58 raspberrypi streaming-i2c-bma280[15318]: BMA280.Results [x= 0.0010g, y= 0.9300g, z=-0.0104g, temp=22.50°C]


[info] Oct 01 22:21:59 raspberrypi streaming-i2c-bma280[15318]: BMA280.Results [x=-0.0158g, y= 0.9307g, z=-0.0139g, temp=22.50°C]
[info] Closing session to raspberrypi.local
```

The BMA280 class logs by itself the result of both the initialization (outputting the Chip ID), the selt-test, and the Calibration. 
Then our timer issues READ commands, and the response is logged to the console.

### Running this example on a Raspberry Pi

RIoT comes with a tool (RIoT Control) which simplifies deployment to your Raspberry Pi. This tool is already preconfigured in this project. Set-up the name of your device and the user credentials to use for deployment in the build.sbt file:

```
riotTargets := Seq(
  riotTarget("raspberrypi", "pi", "raspberry")
)
```
Then connect your Device directly on a free port on your development machine, or on your local network. Then run <code>sbt riotRun</code> to compile your code, copy it to the device, and run it locally. 
The result will appear in your console. Press 'Enter' twice to stop the Program on your Pi. 

[BMA280]:https://www.bosch-sensortec.com/bst/products/all_products/bma280