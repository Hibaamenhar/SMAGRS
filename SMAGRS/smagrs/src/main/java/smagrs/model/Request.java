package smagrs.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashSet;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Request {
    public String requester;
    public TimeSlot slot;
    public int capacity;
    /** Équipements normalisés (minuscule, sans espaces) */
    public Set<String> equipment = new LinkedHashSet<>();
    public String purpose;

    public Request() {}

    /** Accepte un tableau JSON ["projecteur","tableau"] ou une chaîne "projecteur,tableau". */
    @JsonSetter("equipment")
    public void setEquipment(JsonNode node) {
        Set<String> out = new LinkedHashSet<>();
        if (node == null || node.isNull()) { this.equipment = out; return; }

        if (node.isArray()) {
            for (JsonNode it : node) {
                String v = it.asText("").trim().toLowerCase();
                if (!v.isEmpty()) out.add(v);
            }
        } else {
            // fallback: CSV
            for (String s : node.asText("").split(",")) {
                String v = s.trim().toLowerCase();
                if (!v.isEmpty()) out.add(v);
            }
        }
        this.equipment = out;
    }
}
