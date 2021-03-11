package sample;

import javafx.beans.property.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Class Appointment, which contains information about an appointment picked by a client:
 * date and time of the appointment, and also client's data: first name, surname, phone number
 * and extra comments about made appointment.
 */

public class Appointment implements Serializable {
    private transient SimpleObjectProperty<LocalDate> date;
    private transient SimpleObjectProperty<LocalTime> time;
    private transient SimpleBooleanProperty state;
    private transient SimpleStringProperty name;
    private transient SimpleStringProperty lastName;
    private transient SimpleStringProperty phoneNumber;
    private transient SimpleStringProperty comments;
    private static final long serialVersionUID = 1L;

    public Appointment() { };

    public Appointment(LocalTime time, boolean state, String name, String lastName, String phoneNumber, String comments) {
        this.date = new SimpleObjectProperty<LocalDate>(LocalDate.now().plusDays(1));
        this.time = new SimpleObjectProperty<LocalTime>(time);
        this.state = new SimpleBooleanProperty(state);
        this.name = new SimpleStringProperty(name);
        this.lastName = new SimpleStringProperty(lastName);
        this.phoneNumber = new SimpleStringProperty(phoneNumber);
        this.comments = new SimpleStringProperty(comments);
    }

    public LocalDate getDate() {
        return this.date.get();
    }

    public void setDate(LocalDate date){
        this.date.set(date);
    }

    public ObjectProperty<LocalDate> dateProperty() {
        return date;
    }


    public LocalTime getTime(){
        return this.time.get();
    }

    public void setTime(LocalTime time){
        this.time.set(time);
    }

    public ObjectProperty<LocalTime> timeProperty () {
        return time;
    }


    public String getName(){
        return this.name.get();
    }

    public void setName(String name){
        this.name.set(name);
    }

    public StringProperty nameProperty() {
        return name;
    }


    public String getLastName(){
        return this.lastName.get();
    }

    public void setLastName(String lastName){
        this.lastName.set(lastName);
    }

    public final StringProperty lastNameProperty() {
        return this.lastName;
    }


    public final String getPhoneNumber(){
        return this.phoneNumber.get();
    }

    public final void setPhoneNumber(String phoneNumber){
        this.phoneNumber.set(phoneNumber);
    }

    public final StringProperty phoneNumberProperty() {
        return this.phoneNumber;
    }


    public final String getComments(){
        return this.comments.get();
    }

    public final void setComments(String comments){
        this.comments.set(comments);
    }

    public final StringProperty commentsProperty() {
        return this.comments;
    }


    public final boolean getState() {
        return this.state.get();
    }

    public final void setState(boolean s) {
        this.state.set(s);
    }

    public final BooleanProperty stateProperty() {
        return this.state;
    }


    public String toString() {
        return date.toString() + " " + time.toString() + " " + getName() + " " + getLastName() + " " + getState()  + " " + getPhoneNumber()  + " " + getComments();
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeObject(getDate());
        s.writeObject(getTime());
        s.writeBoolean(getState());
        s.writeUTF(getName());
        s.writeUTF(getLastName());
        s.writeUTF(getPhoneNumber());
        s.writeUTF(getComments());
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {

        this.date = new SimpleObjectProperty<LocalDate>((LocalDate) s.readObject());
        this.time = new SimpleObjectProperty<LocalTime>((LocalTime) s.readObject());
        this.state = new SimpleBooleanProperty(s.readBoolean());
        this.name = new SimpleStringProperty(s.readUTF());
        this.lastName = new SimpleStringProperty(s.readUTF());
        this.phoneNumber = new SimpleStringProperty(s.readUTF());
        this.comments = new SimpleStringProperty(s.readUTF());

    }

}
