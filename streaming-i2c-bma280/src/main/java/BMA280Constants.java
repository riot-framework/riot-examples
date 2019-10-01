public interface BMA280Constants {
    /*
     * Local addresses
     */
    static final int BGW_CHIPID = 0x00;
    static final int ACCD_X_LSB = 0x02;
    static final int ACCD_X_MSB = 0x03;
    static final int ACCD_Y_LSB = 0x04;
    static final int ACCD_Y_MSB = 0x05;
    static final int ACCD_Z_LSB = 0x06;
    static final int ACCD_Z_MSB = 0x07;
    static final int ACCD_TEMP = 0x08;
    static final int INT_STATUS_0 = 0x09;
    static final int INT_STATUS_1 = 0x0A;
    static final int INT_STATUS_2 = 0x0B;
    static final int INT_STATUS_3 = 0x0C;
    static final int FIFO_STATUS = 0x0E;
    static final int PMU_RANGE = 0x0F;
    static final int PMU_BW = 0x10;
    static final int PMU_LPW = 0x11;
    static final int PMU_LOW_NOISE = 0x12;
    static final int ACCD_HBW = 0x13;
    static final int BGW_SOFTRESET = 0x14;
    static final int INT_EN_0 = 0x16;
    static final int INT_EN_1 = 0x17;
    static final int INT_EN_2 = 0x18;
    static final int INT_MAP_0 = 0x19;
    static final int INT_MAP_1 = 0x1A;
    static final int INT_MAP_2 = 0x1B;
    static final int INT_SRC = 0x1E;
    static final int INT_OUT_CTRL = 0x20;
    static final int INT_RST_LATCH = 0x21;
    static final int INT_0 = 0x22;
    static final int INT_1 = 0x23;
    static final int INT_2 = 0x24;
    static final int INT_3 = 0x25;
    static final int INT_4 = 0x26;
    static final int INT_5 = 0x27;
    static final int INT_6 = 0x28;
    static final int INT_7 = 0x29;
    static final int INT_8 = 0x2A;
    static final int INT_9 = 0x2B;
    static final int INT_A = 0x2C;
    static final int INT_B = 0x2D;
    static final int INT_C = 0x2E;
    static final int INT_D = 0x2F;
    static final int FIFO_CONFIG_0 = 0x30;
    static final int PMU_SELF_TEST = 0x32;
    static final int TRIM_NVM_CTRL = 0x33;
    static final int BGW_SPI3_WDT = 0x34;
    static final int OFC_CTRL = 0x36;
    static final int OFC_SETTING = 0x37;
    static final int OFC_OFFSET_X = 0x38;
    static final int OFC_OFFSET_Y = 0x39;
    static final int OFC_OFFSET_Z = 0x3A;
    static final int TRIM_GP0 = 0x3B;
    static final int TRIM_GP1 = 0x3C;
    static final int FIFO_CONFIG_1 = 0x3E;
    static final int FIFO_DATA = 0x3F;

    public static final int DEFAULT_ADDRESS = 0x18; // when ADO is LOW
    public static final int ALTERNATE_ADDRESS = 0x19; // when ADO is HIGH

    /**
     * Accelerometer Scale
     */
    public enum AccelerometerScale {
        AFS_2G(0x02), AFS_4G(0x05), AFS_8G(0x08), AFS_16G(0x0C);

        AccelerometerScale(int value) {
            this.value = (byte) value;
        }

        final byte value;
    }

    /**
     * Bandwidth
     */
    public enum Bandwidth {
        BW_7_81Hz(0x08), // 15.62 Hz sample rate, etc
        BW_15_63Hz(0x09), //
        BW_31_25Hz(0x0A), //
        BW_62_5Hz(0x0B), //
        BW_125Hz(0x0C), // 250 Hz sample rate
        BW_250Hz(0x0D), //
        BW_500Hz(0x0E), //
        BW_1000Hz(0x0F); // 2 kHz sample rate == unfiltered data

        Bandwidth(int value) {
            this.value = (byte) value;
        }

        final byte value;
    }

    /**
     * Power Mode
     */
    public enum PowerMode {
        normal_Mode(0x00), //
        deepSuspend_Mode(0x01), //
        lowPower_Mode(0x02), //
        suspend_Mode(0x04);

        PowerMode(int value) {
            this.value = (byte) value;
        }

        final byte value;
    }

    /**
     * Sleep Duration in Low-Power Mode
     */
    public enum SleepDuration {
        sleep0_5ms(0x05), sleep1ms(0x06), sleep2ms(0x07), sleep4ms(0x08), sleep6ms(0x09), sleep10ms(0x0A), sleep25ms(
                0x0B), sleep50ms(0x0C), sleep100ms(0x0D), sleep500ms(0x0E), sleep1000ms(0x0F);

        SleepDuration(int value) {
            this.value = (byte) value;
        }

        final byte value;
    }
}
