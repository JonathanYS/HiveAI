module com.example.hive {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens com.example.hive to javafx.fxml;
    exports com.example.hive;
}