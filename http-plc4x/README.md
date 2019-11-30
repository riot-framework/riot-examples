http-plc4x
----------------

### Preparations
This example shows how to connect to a Siemens S7 PLC through Ethernet (e.g. using a CP 243-1), and expose its functionality as JSON webservices.
 
Depending on what model you are using, a different connection string will be needed, and at the very least you will need 
to correct the IP address used in the example code, in the `PlcServer` class:

```
static final String connectionString = "s7://192.168.1.222/0/2?controller-type=S7_300";
```
`s7` refers to the Siemens S7 protocol. The IP Address is that of your 'Communication Processor'. The two next components, `0` and `2`,
are the Rack and Slot address of your siemens PLC (this part will be different for other PLC types). This will depend on
the type and configuration of your PLC, but some common values are:

- S7 200: Rack 0, Slot 0
- S7 300: Rack 0, Slot 0
- S7 400: Depending on the configuration
- S7 1200: Rack 0, Slot 0 or Slot 1
- S7 1500: Rack 0, Slot 0 or Slot 1

The key-value pairs after the `?` are additional parameters that are passed to the PLC4X driver. In this case, `controller-type=S7_300`
is a hint the driver needs because it fails to autodetect my PLC (The 200 and 300 series are equivatent for the PLC4X Driver).

Refer to the [PLC4X documentation](http://plc4x.apache.org/plc4j/plc4j-protocols/developers/implementing-drivers.html) for 
more information about connection strings. Also, the [Sharp7 project page](http://snap7.sourceforge.net/sharp7.html) has some
good information about the settings for various types of S7 CPUs.

The example assumes that:

- a 'Control Box', containing 4 buttons, is connected to the inputs I1.0 through I1.3 (as seen in the picture below)
- a stack of 3 status lights will be connected to outputs Q0.0 through Q0.2 (missing in the picture)

![example setup](https://github.com/riot-framework/riot-examples/raw/master/http-plc4x/example.jpg)

But it will work without either: If no switches  are connected to the inputs, their state will always be "low". If no lights are connected to the outut, the state of the
lights can still be seen on the S7 PLC's diagnostic lights.

### The Mappings: PLC to Java Object, then Object to JSON

The example maps individual 'fields' from the PLC to fields in a simple Java class (a POJO, 'Plain Old Java Object') using annotations.
Data is read from the PLC into instances of this class, and made available via HTTP, serialised in a JSON document (or the other way
around: Received via HTTP, serialised into an instance of the POJO, then sent to the PLC). The `MyControlBox` class, for example,
models the state of the gray control box with 4 buttons seen in the above picture:

```java
import org.apache.plc4x.java.opm.PlcEntity;
import org.apache.plc4x.java.opm.PlcField;
import java.util.Objects;

@PlcEntity
public class MyControlBox {

    @PlcField("%I1.0:BOOL")
    public boolean high;

    @PlcField("%I1.1:BOOL")
    public boolean start;

    @PlcField("%I1.2:BOOL")
    public boolean stop;

    @PlcField("%I1.3:BOOL")
    public boolean emergency_stop;

}
```

In this example, the `emergency_stop` field will be mapped to the state of the input 3 in the block 1, and is mapped to a `bolean`, 
meaning it's `true` when the emergency stop button is pressed, false otherwise.

This same object will then be serialized using JSON when accessed through the web service (e.g. by pointing your browser to
`http://localhost:8080/controlbox`), and the result will look like this:
```json
{
  emergency_stop: true,
  high: false,
  start: false,
  stop: false
}
```

In this case, all buttons are in their resting position, but the emergency stop button is pressed.

### Code walk-through 

The entire web service is realised in the `PlcServer` class, in about 75 lines of code. The main class, as in the other examples,
creates the Akka `ActorSystem` and `Materializer` but also an Akka HTTP server (①).

This time, the flow is created by Akka HTTP using `Routes` (②), which are defined in a separate method (explained next). The
Akka HTTP server is started using these flows (③).

Note that all threads are daemons at this point: If the main method terminates, the server is shut down. For this reason, we need
to block indefinitely (④).

```java
    public static void main(String[] args) throws InterruptedException {
        ActorSystem system = ActorSystem.create("plc-server");
        Materializer mat = ActorMaterializer.create(system);
     ① Http http = Http.get(system);

     ② final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = new PlcServer().createRoute().flow(system, mat);
     ③ final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost("localhost", 8080), mat);

     ④ Thread.currentThread().join();
    }
```

The routes are defined in the `createRoute` method. Setting up routes is described in details in the [Akka HTTP documentation](https://doc.akka.io/docs/akka-http/current/routing-dsl/routes.html).
For this example, let's just observe that we set up two routes:
- one on the path `/controlbox` accepts GET requests (①), and serves them by executing the  `getControlBoxState()` method: This will
query the state of our control box, and return it as JSON object.
- another, on the path `/status`, accepts POST requests (②), and serves them using the method `postStatusLights(json)`. This will set
the state of the 3 status lights.

```java
private Route createRoute() {
    return concat(
         ① path("controlbox", () -> get( 
                    () -> getControlBoxState())
            ),
         ② path("status", () -> post(
                    () -> entity(Jackson.unmarshaller(MyStatusLights.class), this::postStatusLights))
            )
    );
}
```

The `getControlBoxState()` methods execute PLC4X's `EntityManager.read(...)` method asynchronously (①), and return a `Future` (②) that will
complete successfully and yield an HTTP response 200 if and when the read method completes successfully:
```java 
private Route getControlBoxState() {
    CompletionStage<MyControlBox> future = CompletableFuture.supplyAsync(new Supplier<MyControlBox>() {
        @Override
        public MyControlBox get() {
            try {
     ①         return entityManager.read(MyControlBox.class, connectionString);
            } catch (OPMException e) {
                throw new RuntimeException(e);
            }
        }
    });

    return onSuccess(future, done ->
     ②     completeOKWithFuture(future, Jackson.marshaller())
    );
}
```

The `postStatusLights(state)` methods work similarly, using PLC4X's `EntityManager.write(...)` method (①), and return a `Future` (②) that will
yield an HTTP 200 response, containing the current state of the output as a JSON object (②):
```java
private Route postStatusLights(MyStatusLights state) {
    CompletionStage<MyStatusLights> future = CompletableFuture.supplyAsync(new Supplier<MyStatusLights>() {
        @Override
        public MyStatusLights get() {
            try {
         ①     return entityManager.write(MyStatusLights.class, connectionString, state);
            } catch (OPMException e) {
                throw new RuntimeException(e);
            }
        }
    });

    return onSuccess(future, done ->
        ②  completeOKWithFuture(future, Jackson.marshaller())
    );
}
```

If the execution throws a `RuntimeException` instead, the server will reply with an HTTP 500. The `OPMException` raised by PLC4X
must be caught: It wouldn't be identified by Akka HTTP as a non-critical exception, and would cause the server to restart.

### Mappting annotations: PLC to Java object

The `@PlcEntity` marks this class as being instantiable by PLC4X from a PLC's state. The 4 fields, `high`, `start`, `stop`, and `emergency_stop`,
will contain the state of the black speed switch, the green button, the red button, and the twist-to-reset red emergency stop button. These are connected
to the inputs I1.0 through I1.3 on the PLC, and can only have two states: open or closed. This is indicated by the `@PlcField` annotation, which
specifies the field and type to use. For S7 PLCs, the fields can be:

- `%I` for Input fields, followed by the port number (that should be written on your PLC, besides the input terminal), e.g. "%I1.0" as above for the first bit of the second input block, or "%0" for the entire byte resulting from the 8 bits of the first input block
- `%Q` for Output fields (not `O`!), followed by the port number (similarly, written on the PLC, besides the output terminal), e.g. "%Q0" for the 8 output bits of your S200 CPU, or "Q124" for the first output of your S300 CPU (provided it has outputs).  
- `%F` for Flags used when programming the PLC
- `%DB` for Data Block, followed by the block number, a period, and the offset from which you want to read, e.g. "%DB05.10" for the tenth byte in data block 5 (won't work on an S200, which only has 1 block), or "%DB01.0" for the first byte of the first block (works on an S200). 
- `%V` for data from Data Block 1, e.g. "V100" for the hundredth byte in an S200's memory, or the hundredth byte in the S300's data block 1. 
 
The type is specified after a colon (`:`).  For S7 PLCs, these can be:

- BOOL (1 bit input as java boolean)
- BYTE (1 byte as 8 bits)
- WORD (2 byte as 16 bits)
- DWORD (4 byte as 32 bits)
- LWORD (8 byte as 64 bits)
- SINT (8 bits as signed Byte)
- USINT (8 bits as unsigned Short)
- INT (16 bits as signed Short)
- UINT (16 bits as unsigned Integer)
- DINT (32 bits as signed Integer)
- UDINT (32 bits as unsigned Long)
- LINT (64 bits as signed Long)
- ULINT (64 bits as unsigned BigInteger)
- REAL (as java Float)
- LREAL (as java Double)
- CHAR (fixed-length UTF-8 data as String)
- WCHAR (fixed-length UTF-16 data as String)
- STRING (variable-length UTF-8 data as String)
- WSTRING (variable-length UTF-16 data as String)
- DATE_AND_TIME (date/time as Java `LocalDateTime`)
- TIME_OF_DAY (time as Java `LocalTime`)
- DATE (date as Java `LocalDate`)

In the example above, the `emergency_stop` field is the state of the input 3 in the block 1, and is mapped to a `bolean`, 
meaning it's `true` when the emergency stop button is pressed, false otherwise.

### Mappting annotations: Java object to JSON

This example uses Jackson as the JSON serializer. Any public member will be serialised in the JSON response, and no annotation is strictly necessary.
In fact, the `MyControlBox` class doesn't contain any JSON Annotations.

The `MyStatusLights` must be instantiated by Jackson, and JSON fields must map to it, To do so, a Constructor was annotated with Jackson annotations, 
containing the field mapping information (①). The fields themselves were mapped to the PLC's output field, using PLC4X's annotations (②). 

```java
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.plc4x.java.opm.PlcEntity;
import org.apache.plc4x.java.opm.PlcField;

public class MyStatusLights {

    public MyStatusLights() {
    }

    @JsonCreator
    public MyStatusLights(@JsonProperty("red") boolean red,
                   @JsonProperty("yellow") boolean yellow,
         ①        @JsonProperty("green") boolean green) {

        this.red = red;
        this.yellow = yellow;
        this.green = green;
    }

    @PlcField("%Q0.0:BOOL") ②
    public boolean red;

    @PlcField("%Q0.1:BOOL") ②
    public boolean yellow;

    @PlcField("%Q0.2:BOOL") ②
    public boolean green;

}
```

Combining both annotations allows the mapping to a JSON object and to the PLCs fields to change independently of each other.

### Testing the web services

To test the `controlbox` service, simply point your browser to `http://localhost:8080/controlbox`, you should see the following result:

```json
{
  emergency_stop: true,
  high: false,
  start: false,
  stop: false
}
```

To control the lights on the light tower (or to just toggle the outputs on your S7), use an HTTP test client such as Postman, and send a POST 
request to `http://localhost:8080/status` with the following raw payload, of type `application/json':

```json
{
"red": false,
"yellow": true,
"green": false
}
```

Or use the Curl program from a terminal window:

```shell script
curl -H "Content-Type: application/json" -X POST -d '{ "red": false, "yellow": true, "green": false }' http://localhost:8080/status
```