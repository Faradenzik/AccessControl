module by.farad.accesscontrol {
    requires javafx.controls;
    requires javafx.fxml;

    exports by.farad.accesscontrol;
    exports by.farad.accesscontrol.controllers;

    opens by.farad.accesscontrol.controllers to javafx.fxml;
}
