package org.launchcode.models;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.sql.Date;

@Entity
public class Cleaning implements Comparable<Cleaning> {

    @Id
    @GeneratedValue
    private int id;

    @NotNull(message = "Please choose a date")
    private Date date;              // have to use java.sql.date, not LocalDate :(

    @Size(max=22, message = "Notes too long")
    private String notes;

    @ManyToOne
    private Room room;

    private boolean old = false;    // maybe change this to int, -# is old, 0 is today, +# is future
                                    // e.g. -2 is 2 days ago, 1 is tomorrow, 7 is a week from today

    public Cleaning(Date date) {
        this.date = date;
    }

    public Cleaning(Date date, Room room) {

        this.date = date;
        this.room = room;

    }

    public Cleaning() {}

    public int getId() {
        return id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isOld() {
        return old;
    }

    public void setOld(boolean old) {
        this.old = old;
    }

    @Override
    public int compareTo(Cleaning o) {
        return getDate().compareTo(o.getDate());
    }

}
