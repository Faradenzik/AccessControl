package by.farad.accesscontrol.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javafx.scene.image.Image;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class Worker {
    private Long id;
    private String name;
    private String surname;
    private String patronymic;
    private String sex;
    private LocalDate birthday;
    private String phone;
    private String position;
    private String otdel;
    private String photoFile;
    private String passId;
    @JsonIgnore
    private Image photo;
    @JsonIgnore
    private List<AccessGroup> groups;
}