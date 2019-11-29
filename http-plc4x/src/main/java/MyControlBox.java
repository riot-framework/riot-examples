import org.apache.plc4x.java.opm.PlcEntity;
import org.apache.plc4x.java.opm.PlcField;

import java.util.Objects;

/**
 * Example of a PLC Entity: Describes the state of a control box containing several buttons and switches, connected to a
 * PLC's inputs.
 */
@PlcEntity
public class MyControlBox {

    /**
     * Whether the speed selector switch is in the HIGH position. False if the switch is in the LOW position.
     */
    @PlcField("%I1.0:BOOL")
    public boolean high;

    /**
     * Whether the Start button is pressed.
     */
    @PlcField("%I1.1:BOOL")
    public boolean start;

    /**
     * Whether the Stop button  is pressed.
     */
    @PlcField("%I1.2:BOOL")
    public boolean stop;

    /**
     * Whether the Emergency Stop (twist-to-reset type) button is currently engaged.
     */
    @PlcField("%I1.3:BOOL")
    public boolean emergency_stop;

    @Override
    public String toString() {
        return "MyMachine{" +
                "high=" + high +
                ", start=" + start +
                ", stop=" + stop +
                ", emergency_stop=" + emergency_stop +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MyControlBox myControlBox = (MyControlBox) o;
        return high == myControlBox.high &&
                start == myControlBox.start &&
                stop == myControlBox.stop &&
                emergency_stop == myControlBox.emergency_stop;
    }

    @Override
    public int hashCode() {
        return Objects.hash(high, start, stop, emergency_stop);
    }
}
