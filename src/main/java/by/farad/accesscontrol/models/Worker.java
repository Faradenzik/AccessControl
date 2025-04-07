package by.farad.accesscontrol.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.*;

import java.time.LocalDate;

public class Worker {
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty surname = new SimpleStringProperty();
    private final StringProperty patronomic = new SimpleStringProperty();
    private final StringProperty sex = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> birthday = new SimpleObjectProperty<>();
    private final StringProperty phone = new SimpleStringProperty();
    private final StringProperty position = new SimpleStringProperty();
    private final StringProperty otdel = new SimpleStringProperty();
    private final StringProperty department = new SimpleStringProperty();

    public Worker() {
        // Пустой конструктор для Jackson
    }

    public Worker(long id, String name, String surname, String patronomic,
                  String sex, LocalDate birthday, String phone,
                  String position, String otdel, String department) {
        this.id.set(id);
        this.name.set(name);
        this.surname.set(surname);
        this.patronomic.set(patronomic);
        this.sex.set(sex);
        this.birthday.set(birthday);
        this.phone.set(phone);
        this.position.set(position);
        this.otdel.set(otdel);
        this.department.set(department);
    }

    // Геттеры для свойств (Property)
    public LongProperty idProperty() { return id; }
    public StringProperty nameProperty() { return name; }
    public StringProperty surnameProperty() { return surname; }
    public StringProperty patronomicProperty() { return patronomic; }
    public StringProperty sexProperty() { return sex; }
    public ObjectProperty<LocalDate> birthdayProperty() { return birthday; }
    public StringProperty phoneProperty() { return phone; }
    public StringProperty positionProperty() { return position; }
    public StringProperty otdelProperty() { return otdel; }
    public StringProperty departmentProperty() { return department; }

    // Стандартные геттеры (для JSON)
    @JsonProperty("id")
    public long getId() { return id.get(); }

    @JsonProperty("name")
    public String getName() { return name.get(); }

    @JsonProperty("surname")
    public String getSurname() { return surname.get(); }

    @JsonProperty("patronomic")
    public String getPatronomic() { return patronomic.get(); }

    @JsonProperty("sex")
    public String getSex() { return sex.get(); }

    @JsonProperty("birthday")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    public LocalDate getBirthday() { return birthday.get(); }

    @JsonProperty("phone")
    public String getPhone() { return phone.get(); }

    @JsonProperty("position")
    public String getPosition() { return position.get(); }

    @JsonProperty("otdel")
    public String getOtdel() { return otdel.get(); }

    @JsonProperty("department")
    public String getDepartment() { return department.get(); }

    // Сеттеры
    public void setId(long id) { this.id.set(id); }
    public void setName(String name) { this.name.set(name); }
    public void setSurname(String surname) { this.surname.set(surname); }
    public void setPatronomic(String patronomic) { this.patronomic.set(patronomic); }
    public void setSex(String sex) { this.sex.set(sex); }
    public void setBirthday(LocalDate birthday) { this.birthday.set(birthday); }
    public void setPhone(String phone) { this.phone.set(phone); }
    public void setPosition(String position) { this.position.set(position); }
    public void setOtdel(String otdel) { this.otdel.set(otdel); }
    public void setDepartment(String department) { this.department.set(department); }

    @Override
    public String toString() {
        return String.format("%s %s %s (%s)", getSurname(), getName(), getPatronomic(), getPosition());
    }
}