package by.farad.accesscontrol.models;

import lombok.Data;

import java.time.LocalTime;

@Data
public class AccessTimeRange {
    private Long id;
    private Room room;
    private Worker worker;
    private AccessGroup group;
    private String daysOfWeek;
    private LocalTime timeStart;
    private LocalTime timeEnd;
}
