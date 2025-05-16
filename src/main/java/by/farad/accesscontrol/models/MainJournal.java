package by.farad.accesscontrol.models;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MainJournal {
    private Long id;
    private LocalDateTime time;
    private Worker worker;
    private Room room;
    private String direction;
    private String status;
    private String description;
}
