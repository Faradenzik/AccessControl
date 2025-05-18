module by.farad.accesscontrol {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires com.fasterxml.jackson.datatype.jsr310;

    exports by.farad.accesscontrol.models;
    requires com.fasterxml.jackson.databind;
    requires static lombok;
    requires com.gluonhq.charm.glisten;
    requires org.controlsfx.controls;
    requires java.desktop;
    requires spring.boot.autoconfigure;
    exports by.farad.accesscontrol;
    exports by.farad.accesscontrol.controllers;

    opens by.farad.accesscontrol.controllers to javafx.fxml;
    exports by.farad.accesscontrol.controllers.access;
    opens by.farad.accesscontrol.controllers.access to javafx.fxml;
    exports by.farad.accesscontrol.controllers.workers;
    opens by.farad.accesscontrol.controllers.workers to javafx.fxml;
    exports by.farad.accesscontrol.controllers.rooms;
    opens by.farad.accesscontrol.controllers.rooms to javafx.fxml;
    exports by.farad.accesscontrol.controllers.groups;
    opens by.farad.accesscontrol.controllers.groups to javafx.fxml;
    exports by.farad.accesscontrol.controllers.users;
    opens by.farad.accesscontrol.controllers.users to javafx.fxml;
}
