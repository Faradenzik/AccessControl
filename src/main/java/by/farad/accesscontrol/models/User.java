package by.farad.accesscontrol.models;

import lombok.Data;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String role;
    private Worker worker;
}
