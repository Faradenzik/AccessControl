package by.farad.accesscontrol.models;

public class Worker {
    private int id;
    private String name;
    private String surname;
    private String patronomyc;
    private String sex;
    private String birthday;
    private String position;
    private String department;

    public Worker () {

    }

    public Worker (int id, String name, String surname, String patronomyc, String sex, String birthday, String position, String department) {
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.patronomyc = patronomyc;
        this.sex = sex;
        this.birthday = birthday;
        this.position = position;
        this.department = department;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getPatronomyc() {
        return patronomyc;
    }

    public void setPatronomyc(String patronomyc) {
        this.patronomyc = patronomyc;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
