package smagrs.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeSlot {
    public LocalDateTime start;

    /** durée en heures (ex: 2). Accepte aussi "hours" venant de l’UI. */
    @JsonAlias({"hours"})
    public int duration;

    public TimeSlot() {}
    public TimeSlot(LocalDateTime start, int duration) {
        this.start = start;
        this.duration = duration;
    }

    public boolean overlaps(TimeSlot other) {
        var end  = start.plusHours(duration);
        var oEnd = other.start.plusHours(other.duration);
        return !end.isBefore(other.start) && !oEnd.isBefore(this.start);
    }

    @Override public String toString() { return start + " +" + duration + "h"; }
}
