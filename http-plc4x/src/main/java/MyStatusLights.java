import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.plc4x.java.opm.PlcEntity;
import org.apache.plc4x.java.opm.PlcField;

import java.util.Objects;

/**
 * Example of a PLC Entity: Describes the state of a stack light, with 3 status lights, connected to a PLC's output.
 */
@PlcEntity
public class MyStatusLights {

    @JsonCreator
    MyStatusLights(@JsonProperty("red") boolean red,
                   @JsonProperty("yellow") boolean yellow,
                   @JsonProperty("green") boolean green) {

        this.red = red;
        this.yellow = yellow;
        this.green = green;
    }

    /**
     * Whether the topmost, red light is on.
     */
    @PlcField("%Q0.0:BOOL")
    public boolean red;

    /**
     * Whether the middle, yellow light is on.
     */
    @PlcField("%Q0.1:BOOL")
    public boolean yellow;

    /**
     * Whether the lowest, green light is on.
     */
    @PlcField("%Q0.2:BOOL")
    public boolean green;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MyStatusLights that = (MyStatusLights) o;
        return red == that.red &&
                yellow == that.yellow &&
                green == that.green;
    }

    @Override
    public int hashCode() {
        return Objects.hash(red, yellow, green);
    }

    @Override
    public String toString() {
        return "MyStatusLights{" +
                "red=" + red +
                ", yellow=" + yellow +
                ", green=" + green +
                '}';
    }
}
