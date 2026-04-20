package app;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.*;
import model.Donor;

import javafx.scene.control.cell.PropertyValueFactory;
public class DonorController {

    @FXML private TableView<Donor> donorTable;
    @FXML private TableColumn<Donor, String> nameCol;
    @FXML private TableColumn<Donor, String> bloodCol;
    @FXML private TableColumn<Donor, Integer> priorityCol;

    @FXML
    public void initialize() {

        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        bloodCol.setCellValueFactory(new PropertyValueFactory<>("bloodType"));
        priorityCol.setCellValueFactory(new PropertyValueFactory<>("priority"));

        ObservableList<Donor> list = FXCollections.observableArrayList(
            new Donor("Kasun","A+",1),
            new Donor("Nimal","O+",3),
            new Donor("Saman","B+",2)
        );

        donorTable.setItems(list);
    }
}