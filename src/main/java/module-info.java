module com.example.hive {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires org.pcollections;


    opens com.example.hive to javafx.fxml;
    exports com.example.hive;
    exports com.example.hive.controller;
    opens com.example.hive.controller to javafx.fxml;
    exports com.example.hive.utils;
    opens com.example.hive.utils to javafx.fxml;
}