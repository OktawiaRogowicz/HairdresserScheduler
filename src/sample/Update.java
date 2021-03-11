package sample;

import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import java.io.*;

public class Update extends Thread {

    final InputStream in;
    final OutputStream out;
    final TableView<Appointment> appointmentsTable;
    final ObservableList<Appointment> appointments;

    public Update(InputStream in, OutputStream out, TableView<Appointment> appointmentsTable, ObservableList<Appointment> appointments) {
        this.in = in;
        this.out = out;
        this.appointmentsTable = appointmentsTable;
        this.appointments = appointments;
    }

    @Override
    public void run() {
        while(true) {
            try {
                ObjectOutputStream oout = new ObjectOutputStream(out);
                oout.flush();
                ObjectInputStream oin = new ObjectInputStream(in);
                Appointment a = ((Appointment) oin.readObject());

                if( appointments.get(a.getTime().getHour() - 10).getName().equals("-") ) {
                    // udalo sie
                    oout.writeBoolean(true);

                    appointments.get(a.getTime().getHour() - 10).setName(a.getName());
                    appointments.get(a.getTime().getHour() - 10).setLastName(a.getLastName());
                    appointments.get(a.getTime().getHour() - 10).setPhoneNumber(a.getPhoneNumber());
                    appointments.get(a.getTime().getHour() - 10).setComments(a.getComments());
                    appointmentsTable.refresh();

                } else {
                    // do zmiany, jezeli dodam ten przeklety niedzialajacy boolean ):<
                    // nie udalo sie
                    oout.writeBoolean(false);
                }

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static void sendUpdateold(InputStream in, OutputStream out, Appointment a) {
        try {
            ObjectOutputStream oout = new ObjectOutputStream(out);
            oout.flush();
            ObjectInputStream oin = new ObjectInputStream(in);

            oout.writeObject(a); // wysylanie klikniętego obiektu
            System.out.println("probuje odebrac");
            boolean answer = oin.readBoolean();
            System.out.println("odebrane");

            if( answer == true ) {
                System.out.println("Picked!");
            } else {
                System.out.println("Can't pick this appointment");
            }

            System.out.println("sending...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

