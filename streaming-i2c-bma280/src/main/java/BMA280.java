import akka.util.Timeout;
import com.pi4j.io.i2c.I2CDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import riot.protocols.I2CProtocol;
import riot.protocols.ProtocolDescriptor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * BMA280 protocol. Based on the BMA280 C++ library by Kris Winer (Tlera
 * Corporation).
 * 
 * @see https://github.com/kriswiner/BMA280/blob/master/BMA280Constants.library/BMA280.cpp
 * @see http://www.mouser.com/ds/2/783/BST-BMA280-DS000-11_published-786496.pdf
 */
public class BMA280 implements I2CProtocol<BMA280.Command, BMA280.Results> {
    Logger log = LoggerFactory.getLogger(BMA280.class);

    private final byte accelerometerScale;
    private final byte bandwidth;
    private final byte powerMode;
    private final byte sleepDuration;
    private final float aRes; // Calculated sensor resolution

    /**
     * Commands served by the Actor
     */
    public static enum Command {
        READ, CALIBRATE, SELFTEST
    }

    /**
     * Results of a command execution
     */
    public static class Results {
        /**
         * X-Axis Acceleration (in G)
         */
        float x;

        /**
         * Y-Axis Acceleration (in G)
         */
        float y;

        /**
         * Z-Axis Acceleration (in G)
         */
        float z;

        /**
         * Temperature in °C
         */
        float temp;

        @Override
        public String toString() {
            return String.format("BMA280.Results [x=% .04fg, y=% .04fg, z=% .04fg, temp=%.02f°C]", x, y, z, temp);
        }
    }

    public BMA280() {
        this(BMA280Constants.AccelerometerScale.AFS_16G, BMA280Constants.Bandwidth.BW_125Hz,
                BMA280Constants.PowerMode.normal_Mode, BMA280Constants.SleepDuration.sleep1ms);
    }

    public BMA280(BMA280Constants.AccelerometerScale accelerometerScale, BMA280Constants.Bandwidth bandwidth,
            BMA280Constants.PowerMode powerMode, BMA280Constants.SleepDuration sleepDuration) {
        this.accelerometerScale = accelerometerScale.value;
        this.bandwidth = bandwidth.value;
        this.powerMode = powerMode.value;
        this.sleepDuration = sleepDuration.value;

        switch (accelerometerScale) {
        case AFS_2G:
            aRes = 2.0f / 8192.0f;
            break;
        case AFS_4G:
            aRes = 4.0f / 8192.0f;
            break;
        case AFS_8G:
            aRes = 8.0f / 8192.0f;
            break;
        case AFS_16G:
            aRes = 16.0f / 8192.0f;
            break;
        default:
            throw new AssertionError("Unknown enum value for Accelerometer Scale.");
        }
    }

    @Override
    public ProtocolDescriptor<Command, Results> getDescriptor() {
        return new ProtocolDescriptor<Command, Results>(Command.class, Results.class,
                Timeout.apply(1, TimeUnit.SECONDS));
    }

    @Override
    public void init(I2CDevice dev) throws IOException {
        dev.write(BMA280Constants.BGW_SOFTRESET, (byte) 0xB6);

        delay(100);

        dev.write(BMA280Constants.PMU_RANGE, accelerometerScale);
        dev.write(BMA280Constants.PMU_BW, bandwidth);
        dev.write(BMA280Constants.PMU_LPW, (byte) (powerMode << 5 | sleepDuration << 1));

        // set data ready interrupt (bit 4)
        dev.write(BMA280Constants.INT_EN_1, (byte) 0x10);
        // map data ready interrupt to INT1 (bit 0)
        dev.write(BMA280Constants.INT_MAP_1, (byte) 0x01);
        // interrupts push-pull, active HIGH (bits 0:3)
        dev.write(BMA280Constants.INT_OUT_CTRL, (byte) (0x04 | 0x01));
        // now INT1 can be wired to a GPIO In, which will trigger when data is ready

        final int rawChipID = dev.read(BMA280Constants.BGW_CHIPID);
        log.info("Initialized. Chip ID {}.", String.format("0x%02x", rawChipID));
    }

    @Override
    public Results exec(I2CDevice dev, Command command) throws IOException {
        Results results = new Results();

        switch (command) {
        case SELFTEST:
            byte[] rawData = new byte[2];
            // set full-scale range to 4G
            dev.write(BMA280Constants.PMU_RANGE, BMA280Constants.AccelerometerScale.AFS_4G.value);
            // mg/LSB for 4 g full scale
            float STres = 4000.0f / 8192.0f;

            log.info("Starting self-test");

            // x-axis test
            // positive x-axis
            dev.write(BMA280Constants.PMU_SELF_TEST, (byte) (0x10 | 0x04 | 0x01));
            delay(100);
            dev.read(BMA280Constants.ACCD_X_LSB, rawData, 0, rawData.length);
            int posX = (rawData[1] << 8) | rawData[0];
            // negative x-axis
            dev.write(BMA280Constants.PMU_SELF_TEST, (byte) (0x10 | 0x00 | 0x01));
            delay(100);
            dev.read(BMA280Constants.ACCD_X_LSB, rawData, 0, rawData.length);
            int negX = (rawData[1] << 8) | rawData[0];

            log.info("X-axis self test = {} mg, should be > 800 mg", (float) (posX - negX) * STres / 4.0f);

            // y-axis test
            // positive y-axis
            dev.write(BMA280Constants.PMU_SELF_TEST, (byte) (0x10 | 0x04 | 0x02));
            delay(100);
            dev.read(BMA280Constants.ACCD_Y_LSB, rawData, 0, rawData.length);
            int posY = (rawData[1] << 8) | rawData[0];
            // negative y-axis
            dev.write(BMA280Constants.PMU_SELF_TEST, (byte) (0x10 | 0x00 | 0x02));
            delay(100);
            dev.read(BMA280Constants.ACCD_Y_LSB, rawData, 0, rawData.length);
            int negY = (rawData[1] << 8) | rawData[0];

            log.info("Y-axis self test = {} mg, should be > 800 mg", (float) (posY - negY) * STres / 4.0f);

            // z-axis test
            // positive z-axis
            dev.write(BMA280Constants.PMU_SELF_TEST, (byte) (0x10 | 0x04 | 0x03));
            delay(100);
            dev.read(BMA280Constants.ACCD_Z_LSB, rawData, 0, rawData.length);
            int posZ = (rawData[1] << 8) | rawData[0];
            // negative z-axis
            dev.write(BMA280Constants.PMU_SELF_TEST, (byte) (0x10 | 0x00 | 0x03));
            delay(100);
            dev.read(BMA280Constants.ACCD_Z_LSB, rawData, 0, rawData.length);
            int negZ = (rawData[1] << 8) | rawData[0];

            log.info("Z-axis self test = {} mg, should be > 400 mg", (float) (posZ - negZ) * STres / 4.0f);

            // disable self test
            dev.write(BMA280Constants.PMU_SELF_TEST, (byte) 0x00);
            dev.write(BMA280Constants.PMU_RANGE, accelerometerScale);
            return new Results();
        case CALIBRATE:
            // Fast compensation, as described in datasheet, chapter 4.5.2
            dev.write(BMA280Constants.PMU_RANGE, BMA280Constants.AccelerometerScale.AFS_2G.value);

            // Set target data to 0g, 0g, and +1 g, cutoff at 1% of bandwidth
            dev.write(BMA280Constants.OFC_SETTING, (byte) (0x20 | 0x01));
            // x-axis calibration
            dev.write(BMA280Constants.OFC_CTRL, (byte) (0x20 | 0x01));
            while ((0x10 & dev.read(BMA280Constants.OFC_CTRL)) != 0) {
            }
            // y-axis calibration
            dev.write(BMA280Constants.OFC_CTRL, (byte) (0x40 | 0x01));
            while ((0x10 & dev.read(BMA280Constants.OFC_CTRL)) != 0) {
            }
            // z-axis calibration
            dev.write(BMA280Constants.OFC_CTRL, (byte) (0x60 | 0x01));
            while ((0x10 & dev.read(BMA280Constants.OFC_CTRL)) != 0) {
            }

            // buffer for offset data
            byte[] offsetData = new byte[2];
            dev.read(BMA280Constants.OFC_OFFSET_X, offsetData, 0, offsetData.length);
            float offsetX = ((offsetData[1] << 8) | offsetData[0]) * 7.8125f / 256.0f;
            dev.read(BMA280Constants.OFC_OFFSET_Y, offsetData, 0, offsetData.length);
            float offsetY = ((offsetData[1] << 8) | offsetData[0]) * 7.8125f / 256.0f;
            dev.read(BMA280Constants.OFC_OFFSET_Z, offsetData, 0, offsetData.length);
            float offsetZ = ((offsetData[1] << 8) | offsetData[0]) * 7.8125f / 256.0f;
            log.info("Calibration complete. Offsets: X={}mg, Y={}mg, Z={}mg", offsetX, offsetY, offsetZ);

            // revert to original g-range
            dev.write(BMA280Constants.PMU_RANGE, accelerometerScale);
            // ...then read current value
        default:
            byte[] data = new byte[9];
            dev.read(0, data, 0, data.length);
            final int rawX = (data[BMA280Constants.ACCD_X_MSB] << 8) + data[BMA280Constants.ACCD_X_LSB];
            results.x = (float) rawX * aRes / 4.0f;
            final int rawY = (data[BMA280Constants.ACCD_Y_MSB] << 8) + data[BMA280Constants.ACCD_Y_LSB];
            results.y = (float) rawY * aRes / 4.0f;
            final int rawZ = (data[BMA280Constants.ACCD_Z_MSB] << 8) + data[BMA280Constants.ACCD_Z_LSB];
            results.z = (float) rawZ * aRes / 4.0f;
            final byte rawTemp = data[BMA280Constants.ACCD_TEMP];
            results.temp = 0.5f * ((float) rawTemp) + 23.0f;
        }

        return results;
    }

    @Override
    public void shutdown(I2CDevice dev) throws IOException {
        // Nothing to do here.
    }

    private void delay(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
