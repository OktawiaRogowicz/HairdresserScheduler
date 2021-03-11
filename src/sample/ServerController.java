package sample;

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ResourceBundle;
import javafx.util.Callback;

public class ServerController  implements Initializable {
    @FXML public Circle btnClose;
    @FXML public TextArea messages;
    @FXML public Pane tableViewPane;
    @FXML public StackPane stackPane;

    @FXML public TableView<Appointment> appointmentsTable;
    @FXML public TableColumn<Appointment, LocalDate> dateCol;
    @FXML public TableColumn<Appointment, LocalTime> timeCol;
    @FXML public TableColumn<Appointment, String> nameCol;
    @FXML public TableColumn<Appointment, String> surnameCol;
    @FXML public TableColumn<Appointment, String> phoneNumberCol;
    @FXML public TableColumn<Appointment, String> commentsCol;

    ServerSocket ss = null;
    int numberOfSlots = 8;

    ObservableList<Appointment> appointments = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
    ObservableList<ObjectOutputStream> streams = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

    /**
     * Handles clicks on buttons corresponding to going back (changing the order of StackPanes) or closing the application.
     * @param event
     */

    public void handleMouseEvent(javafx.scene.input.MouseEvent event) {
        if( event.getSource() == btnClose ) {
            System.exit(0);
        }
    }

    /**
     * Creates empty list of appointments.
     * @param appointments
     * @return
     */

    public ObservableList<Appointment> getAppointments(ObservableList<Appointment> appointments) {
        for( int i = 0; i < numberOfSlots; i++ ) {
            Appointment appointment = new Appointment(LocalTime.of(10 + i, 0), false,"-", "-", "-", "-");
            appointments.add(appointment);
        }
        return appointments;
    }

    /**
     *
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

        phoneNumberCol.setCellValueFactory(new PropertyValueFactory<Appointment,String>("phoneNumber"));
        appointments = getAppointments(appointments);
        appointments.addListener(new ListChangeListener<Appointment>() {
            @Override
            public void onChanged(Change<? extends Appointment> a) {
                while (a.next()) {
                    if (a.wasUpdated()) {
                        for (int i = a.getFrom(); i < a.getTo(); ++i) {

                                // metoda wywolywana przy kazdej zmianie
                                for (ObjectOutputStream s : streams) {
                                    try {
                                        s.reset();
                                        s.writeObject(appointments.get(i));
                                        s.flush();
                                        System.out.println("wyslane");
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }

                        }
                    }
                }
            }
        });
        appointmentsTable.setItems(appointments);

        try {
            ss = new ServerSocket(5000);
            Thread t = new WaitingForClients(ss, messages, appointmentsTable, appointments, streams);
            t.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}

/**
 * Thread that is responsible for answering client's socket requests.
 * It runs in the background, waiting for eventual clients. If any is found,
 * then ClientHandler class is called.
 */

class WaitingForClients extends Thread {

    final ServerSocket ss;
    final TextArea messages;
    final ObservableList<Appointment> appointments;
    final TableView<Appointment> appointmentsTable;
    final ObservableList<ObjectOutputStream> streams;

    public WaitingForClients(ServerSocket ss, TextArea messages, TableView<Appointment> appointmentsTable, ObservableList<Appointment> appointments, ObservableList<ObjectOutputStream> streams) {
        this.ss = ss;
        this.messages = messages;
        this.appointmentsTable = appointmentsTable;
        this.appointments = appointments;
        this.streams = streams;
    }

    @Override
    public void run() {

        while(true) {
            Socket s = null;
            try {
                s = ss.accept();

                InputStream in = s.getInputStream();
                OutputStream out = s.getOutputStream();

                ObjectOutputStream oout = new ObjectOutputStream(out);
                oout.flush();
                ObjectInputStream oin = new ObjectInputStream(in);

                streams.add(oout);
                messages.appendText("Client connected: " + s + "\n");

                Thread t = new ClientHandler(s, oin, oout, messages, appointmentsTable, appointments);
                t.start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

/**
 * Class responsible for maintaining one specific client. any signal read from client
 * updates the table right away.
 */

class ClientHandler extends Thread {

    final ObjectInputStream oin;
    final ObjectOutputStream oout;
    final Socket s;
    final TextArea messages;
    final TableView<Appointment> appointmentsTable;
    final ObservableList<Appointment> appointments;

    final int hourIncrement = 10;
    final int numberOfSlots = 8;

    /**
     * The constructor.
     * @param s Client's socket.
     * @param oin Input stream.
     * @param oout Output stream.
     * @param messages TextArea, where messages about client's actions are visible.
     * @param appointmentsTable TableView, where all the info about picked appointments is viewed.
     *      *              As it shows an ObservableList of appointments,
     *      *              fields filled with "-" are synonymous to slots being empty,
     *      *              while slots overwritten with names mean one was picked.
     * @param appointments An ObservableList where all the information about appointments is stored.
     */

    public ClientHandler(Socket s, ObjectInputStream oin, ObjectOutputStream oout, TextArea messages, TableView<Appointment> appointmentsTable, ObservableList<Appointment> appointments) {
        this.s = s;
        this.oin = oin;
        this.oout = oout;
        this.messages = messages;
        this.appointmentsTable = appointmentsTable;
        this.appointments = appointments;
    }

    /**
     * Firstly, thread gives the client information about appointments list right in the moment,
     * so they can create list themselves.
     * Later, it waits for any signal from client that an appointment was either "picked" or "unpicked".
     */

    @Override
    public void run() {

        try {
            int howMany = 0;
            for( int i = 0; i < numberOfSlots; i ++ ) {
                if ( !appointments.get(i).getState()  )
                    howMany++;
            }

            oout.write(howMany);
            oout.flush();

            for( int i = 0; i < howMany; i++ ) {
                oout.writeObject(appointments.get(i));
                oout.flush();
            }

            while(true) {
                Appointment a = ((Appointment) oin.readObject());
                Appointment chosen = appointments.get(a.getTime().getHour() - hourIncrement);

                if ( !chosen.getState() ||
                        (chosen.getState() && chosen.getName().equals(a.getName()) && chosen.getLastName().equals(a.getLastName()) && chosen.getPhoneNumber().equals(a.getPhoneNumber())) ) {

                    chosen.setName(a.getName());
                    chosen.setLastName(a.getLastName());
                    chosen.setPhoneNumber(a.getPhoneNumber());
                    chosen.setComments(a.getComments());
                    appointmentsTable.refresh();
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }
}
