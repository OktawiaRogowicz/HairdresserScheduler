package sample;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.util.Callback;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    public Circle btnClose;
    @FXML
    public StackPane stackPane;
    @FXML
    public Pane tableViewPane;
    @FXML
    public Pane signInPane;
    @FXML
    public Button btnSignIn;
    @FXML
    public ImageView btnBack;
    @FXML
    public TextField phoneNumberField;
    @FXML
    public TextField surnameField;
    @FXML
    public TextField nameField;
    @FXML
    public TextArea messagesTextArea;

    @FXML
    public TableView<Appointment> appointmentsTable;
    @FXML
    public TableColumn<Appointment, LocalDate> dateCol;
    @FXML
    public TableColumn<Appointment, LocalTime> timeCol;
    @FXML
    public TableColumn<Appointment, String> nameCol;
    @FXML
    public TableColumn<Appointment, String> surnameCol;
    @FXML
    public TableColumn<Appointment, String> phoneNumberCol;
    @FXML
    public TableColumn<Appointment, String> commentsCol;
    @FXML
    public Label labelStatus;

    ObservableList<Appointment> appointments = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

    InetAddress ip = InetAddress.getByName("localhost");
    Socket s = new Socket(ip, 5000);
    InputStream in = s.getInputStream();
    OutputStream out = s.getOutputStream();

    int hourIncrement = 10;

    public Controller() throws IOException {
    }

    /**
     * Handles clicks on buttons corresponding to going back (changing the order of StackPanes) or closing the application.
     * @param event Mouse event.
     */

    public void handleMouseEvent(javafx.scene.input.MouseEvent event) {
        if (event.getSource() == btnClose) {
            System.exit(0);
        }
        if (event.getSource() == btnBack) {
            signInPane.toFront();
        }
    }

    /**
     * Handles click on SignIn button.
     * @param event Mouse event.
     */

    public void handleButtonAction(javafx.event.ActionEvent event) {
        if (event.getSource().equals(btnSignIn)) {
            if( !(nameField.getText().equals("-") && surnameField.getText().equals("-") && surnameField.getText().equals("-")) ) {
                tableViewPane.toFront();
                labelStatus.setStyle("-fx-text-fill: #229c5f;");
                labelStatus.setText("Login successful!");
            } else {
                labelStatus.setStyle("-fx-text-fill: #c80000;");
                labelStatus.setText("Login unsuccessful!");
            }
        }
    }

    /**
     * Initialization of TableView and first exchange between the client and the server:
     * the client gets info about server's appointment list, so it can create its own.
     * @param url
     * @param resourceBundle
     */

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        appointmentsTable.setEditable(true);

        dateCol.setCellValueFactory(new PropertyValueFactory<Appointment, LocalDate>("date"));
        timeCol.setCellValueFactory(new PropertyValueFactory<Appointment, LocalTime>("time"));
        nameCol.setCellValueFactory(new PropertyValueFactory<Appointment, String>("name"));
        surnameCol.setCellValueFactory(new PropertyValueFactory<Appointment, String>("lastName"));
        commentsCol.setCellValueFactory(new PropertyValueFactory<Appointment, String>("comments"));

        commentsCol.setCellFactory(TextFieldTableCell.forTableColumn());
        commentsCol.setOnEditCommit(
                new EventHandler<TableColumn.CellEditEvent<Appointment, String>>() {
                    @Override
                    public void handle(TableColumn.CellEditEvent<Appointment, String> t) {
                        ((Appointment) t.getTableView().getItems().get(t.getTablePosition().getRow())).setComments(t.getNewValue());
                    }
                }
        );

        phoneNumberCol.setCellValueFactory(new PropertyValueFactory<Appointment, String>("phoneNumber"));

        try {
            ObjectOutputStream oout = new ObjectOutputStream(out);
            oout.flush();
            ObjectInputStream oin = new ObjectInputStream(in);

            int howMany = oin.read();
            for (int i = 0; i < howMany; i++) {
                appointments.add((Appointment) oin.readObject());
            }

            setTableRowsPickable(oin, oout, appointmentsTable, messagesTextArea);
            appointmentsTable.setItems(appointments);
            appointmentsTable.refresh();

            Thread t = new WaitingForUpdates(oin, oout, messagesTextArea, appointmentsTable, appointments);
            // Main thread responsible for handling updates. See more at the end of the file.
            t.start();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        messagesTextArea.appendText("Client otworzony!\n");
        messagesTextArea.appendText("Connecting...\n");
        messagesTextArea.appendText("Connected!\n");

    }

    /**
     *
     * @param oin Input stream.
     * @param oout Output stream/
     * @param a Information about an appointment picked by client, which will be send to the server.
     * @param messagesTextArea TextArea, where messages about client's actions are visible.
     */

    public static void sendUpdate(ObjectInputStream oin, ObjectOutputStream oout, Appointment a, TextArea messagesTextArea) {
        try {
            messagesTextArea.appendText(a.getName());
            oout.reset();
            oout.writeObject(a); // wysylanie klikniętego obiektu
            oout.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Function that makes the main TableView usable.
     * Adds actions on right click: "Pick" and "Unpick".
     * "Pick", when clicked, picks the appointment and sends a signal to server. See: {@link #sendUpdate(ObjectInputStream, ObjectOutputStream, Appointment, TextArea) sendUpdate}
     * As there is no database connected and the only information about appointments is stored in the appointments list,
     * empty slots are filled with "-". This character, together with a boolean, is synonymous to slot being open.
     * "Unpick", when clicked, fills the slot with "-"s, which is synonymous to being unpicked.
     * @param oin Input stream.
     * @param oout Output stream.
     * @param table TableView, where all the info about picked appointments is viewed.
     *              As it shows an ObservableList of appointments,
     *              fields filled with "-" are synonymous to slots being empty,
     *              while slots overwritten with names mean one was picked.
     * @param messagesTextArea TextArea, where messages about client's actions are visible.
     */

    public void setTableRowsPickable(ObjectInputStream oin, ObjectOutputStream oout, TableView table, TextArea messagesTextArea) {
        table.setRowFactory(
                new Callback<TableView<Appointment>, TableRow<Appointment>>() {
                    @Override
                    public TableRow<Appointment> call(TableView<Appointment> tableView) {
                        final TableRow<Appointment> row = new TableRow<>();
                        final ContextMenu rowMenu = new ContextMenu();
                        MenuItem pickItem = new MenuItem("Pick");
                        pickItem.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
                            @Override
                            public void handle(ActionEvent event) {
                                Appointment app = (Appointment) table.getSelectionModel().getSelectedItem();
                                app.setName(nameField.getText());
                                app.setLastName(surnameField.getText());
                                app.setPhoneNumber(phoneNumberField.getText());
                                app.setState(true);
                                sendUpdate(oin, oout, app, messagesTextArea);
                                appointmentsTable.refresh();
                            }
                        });

                        MenuItem unpickItem = new MenuItem("Unpick");
                        unpickItem.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
                            @Override
                            public void handle(ActionEvent event) {
                                Appointment app = (Appointment) table.getSelectionModel().getSelectedItem();
                                if( app.getName().equals(nameField.getText()) &&
                                    app.getLastName().equals(surnameField.getText()) &&
                                    app.getPhoneNumber().equals(phoneNumberField.getText())) {

                                        app.setName("-");
                                        app.setLastName("-");
                                        app.setPhoneNumber("-");
                                        app.setState(false);
                                        sendUpdate(oin, oout, app, messagesTextArea);
                                        appointmentsTable.refresh();
                                }
                            }
                        });

                        rowMenu.getItems().addAll(pickItem, unpickItem);

                        row.contextMenuProperty().bind(
                                Bindings.when(Bindings.isNotNull(row.itemProperty()))
                                        .then(rowMenu)
                                        .otherwise((ContextMenu) null));
                        return row;
                    }
                });
    }

}

/**
 * Main thread responsible for updating the TableView. Any signal read from server
 * updates the table right away, making it impossible for two clients to try and
 * pick the same appointment, or cancel an appointment of another client.
 */

class WaitingForUpdates extends Thread {

    final ObjectInputStream oin;
    final ObjectOutputStream oout;
    final TextArea messagesTextArea;
    final TableView<Appointment> appointmentsTable;
    final ObservableList<Appointment> appointments;

    final int hourIncrement = 10;

    /**
     * The constructor.
     * @param oin Input stream.
     * @param oout Output stream.
     * @param messagesTextArea TextArea, where messages about client's actions are visible.
     * @param appointmentsTable TableView, where all data about appointments is stored.
     * @param appointments An ObservableList where all the information about appointments is stored.
     */

    public WaitingForUpdates(ObjectInputStream oin, ObjectOutputStream oout, TextArea messagesTextArea, TableView<Appointment> appointmentsTable, ObservableList<Appointment> appointments) {
        this.oin = oin;
        this.oout = oout;
        this.messagesTextArea = messagesTextArea;
        this.appointmentsTable = appointmentsTable;
        this.appointments = appointments;
    }

    @Override
    public void run() {
        while(true) {
            try {
                Appointment a = new Appointment();
                synchronized (oin) {
                    a = (Appointment) oin.readObject();
                }

                Appointment picked = appointments.get(a.getTime().getHour() - hourIncrement);

                picked.setName(a.getName());
                picked.setLastName(a.getLastName());
                picked.setPhoneNumber(a.getPhoneNumber());
                picked.setComments(a.getComments());
                appointmentsTable.refresh();
            } catch ( IOException | ClassNotFoundException e ) {
                e.printStackTrace();
            }
        }
    }
}