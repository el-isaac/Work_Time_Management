import java.sql.Date;
import java.time.LocalTime;

public class records {
    private final Integer id;
    private final Date date;
    private final LocalTime startHour;
    private final LocalTime endHour;
    private final float totalHour;
    private final float dailyEarning;

    public records(Integer id, Date date, LocalTime startHour, LocalTime endHour, float totalHour, float dailyEarning) {
        this.id = id;
        this.date = date;
        this.startHour = startHour;
        this.endHour = endHour;
        this.totalHour = totalHour;
        this.dailyEarning = dailyEarning;
    }

    // Standard getters
    public Integer getId() { return id; }
    public Date getDate() { return date; }
    public LocalTime getStartHour() { return startHour; }
    public LocalTime getEndHour() { return endHour; }
    public float getTotalHour() { return totalHour; }
    public float getDailyEarning() { return dailyEarning; }

    // 👇 Additional getters to match PropertyValueFactory expectations
    public LocalTime getStartTime() { return startHour; }
    public LocalTime getEndTime() { return endHour; }
    public float getTotal() { return totalHour; }
    public float getEarning() { return dailyEarning; }

    // Formatting helpers
    public String getFormattedTotal() {
        return String.format("%.2f", totalHour);
    }

    public String getFormattedEarning() {
        return String.format("€%.2f", dailyEarning);
    }

    public String getSummery() {
        return String.format("%.2f = €%.2f", totalHour, dailyEarning);
    }
}
