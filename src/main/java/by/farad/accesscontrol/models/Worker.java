package by.farad.accesscontrol.models;

import lombok.Data;

import java.time.LocalDate;

@Data
public class Worker {
    private Long id ;
    private String name;
    private String surname;
    private String patronymic;
    private String sex;
    private LocalDate birthday;
    private String phone;
    private String position;
    private String otdel;
    private String department;
}