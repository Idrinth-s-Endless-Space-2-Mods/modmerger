module de.idrinth.endlessspace2.modmerger {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.xml;

    opens de.idrinth.endlessspace2.modmerger to javafx.fxml;
    exports de.idrinth.endlessspace2.modmerger;
}