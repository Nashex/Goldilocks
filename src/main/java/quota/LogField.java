package quota;

public class LogField {

    public String name;
    public int value;
    public long time;

    public LogField(String name, int value) {
        this(name, value, 0L);
    }

    public LogField(String name, int value, long time) {

        this.name = name;
        this.value = value;
        this.time = time;

    }

}
