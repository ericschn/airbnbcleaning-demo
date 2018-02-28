package org.launchcode.controllers;

import org.launchcode.models.Cleaning;
import org.launchcode.models.Room;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.mail.*;
import javax.management.ListenerNotFoundException;

public class GetMail {

    private static final String email_id = "email";       // email address to grab airbnb emails, only gmail tested
    private static final String password = "password";    // password for email address

    // Should return ArrayList of Cleanings to add
    static ArrayList<Cleaning> run(ArrayList<Room> roomList) {

        // Initialize Variables
        ArrayList<Cleaning> cleanings = new ArrayList<>(); // This will be returned if function runs

        // TODO: make run() check timestamp then return list of cleanings to add
        // if (timestamp < 30 minutes) {
        //    return cleanings;  // do not run, return empty ArrayList
        // }


        // Date formatting
        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy");


        // set properties
        Properties properties = new Properties();
        //You can use imap or imaps , *s -Secured
        properties.put("mail.store.protocol", "imaps");
        //Host Address of Your Mail
        properties.put("mail.imaps.host", "imap.gmail.com");
        //Port number of your Mail Host
        properties.put("mail.imaps.port", "993");

        //properties.put("mail.imaps.timeout", "10000");

        try {

            // create a session
            Session session = Session.getDefaultInstance(properties, null);
            // imap store
            Store store = session.getStore("imaps");

            System.out.println("Connecting to gmail");
            // imap connection
            store.connect(email_id, password);
            System.out.println("Connected, checking mail...");

            // grabbing mail from the 'inbox' folder in gmail
            Folder inbox = store.getFolder("inbox");
            // set read and write since we are deleting mail after use
            inbox.open(Folder.READ_WRITE);

            // You've got mail!
            int messageCount = inbox.getMessageCount();
            System.out.println("New messages: " + messageCount);


            // START MAIN LOOP


            for (int i = 1; i < messageCount + 1; i++) {

                Message currentMessage = inbox.getMessage(i); // used for deletion at end of loop

                System.out.println("******************");

                // debug from address
                System.out.println(currentMessage.getFrom()[0].toString());
                // Only emails from these addresses will be scraped
                if (!(currentMessage.getFrom()[0].toString().equals("Email Name <email@email.com>")
                        || currentMessage.getFrom()[0].toString().equals("Email Name <email@email.com>")  )) {

                    currentMessage.setFlag(Flags.Flag.DELETED, true); // don't delete during debugging

                    continue;
                }

                Multipart multipart = (Multipart) currentMessage.getContent();
                // System.out.println("count: " + multipart.getCount());

                String emailBody = "";

                // get multipart message
                for (int j = 0; j < multipart.getCount() - 1; j++) {

                    BodyPart bodyPart = multipart.getBodyPart(j);

                    String disposition = bodyPart.getDisposition();

                    if (disposition != null && (disposition.equalsIgnoreCase("ATTACHMENT"))) { // BodyPart.ATTACHMENT doesn't work for gmail
                        System.out.println("Mail has an attachment");

                        DataHandler handler = bodyPart.getDataHandler();
                        System.out.println("file name : " + handler.getName());
                    } else {
                        // System.out.println("Body: " + bodyPart.getContent());
                        emailBody = bodyPart.getContent().toString();
                    }
                }

                //notodo: account for emails without the hr or hr in the wrong spot, may not be necessary


                //System.out.println(emailBody); // debug email body


                // GETTING PROPERTY INFO


                // First cut off top clutter just in case of forwarding/replies
                emailBody = emailBody.substring(60);

                //System.out.println(emailBody); // debug email body after slicing beginning off

                // Use hr as landmark
                int lineIndex = emailBody.indexOf("------------------------------");

                // gmail renders hr as 30 hyphens, Outlook as 32 underscores...
                if (lineIndex == -1) {
                    lineIndex = emailBody.indexOf("________________________________");
                }

                System.out.println("LINE INDEX: " + lineIndex);

                String propInfo = emailBody.substring(0, lineIndex);

                Room addRoom = findProperty(roomList, propInfo);


                // GETTING DATE INFO


                // Parse the email body to first 'line' plus 200 chars
                emailBody = emailBody.substring(lineIndex, lineIndex + 200); // get slice after first line

                // Location of the comma after the day name, plus 3 to get to start of date text
                int commaIndex = emailBody.indexOf(',');
                emailBody = emailBody.substring(commaIndex + 3);

                // This is a zero width non space character that is between every letter in the dates in the email
                char dum = ((char) 8204);

                emailBody = emailBody.replace(String.valueOf(dum), ""); // removing said character

                //System.out.println(emailBody);

                // get the check in and check out dates as the raw string data
                // using commas as reference points

                int firstCommaIndex = emailBody.indexOf(',');
                int lastCommaIndex = emailBody.lastIndexOf(',');

                String checkIn = emailBody.substring(firstCommaIndex - 7, firstCommaIndex + 6).trim();
                System.out.println("CHECKIN: (" + checkIn + ")");

                // checkout time
                String checkOut = emailBody.substring(lastCommaIndex - 7, lastCommaIndex + 6).trim();
                System.out.println("CHECKOUT: (" + checkOut + ")");

                // parsing them into LocalDates
                LocalDate date1 = LocalDate.parse(checkIn, dtf);
                LocalDate date2 = LocalDate.parse(checkOut, dtf);

                System.out.println(date1.toString());
                System.out.println(date2.toString());

                // Okay we have date and room, now we can make a cleaning
                Date addDate = Date.valueOf(date2);
                Cleaning addCleaning = new Cleaning(addDate, addRoom);

                // Add to ArrayList we're going to return
                cleanings.add(addCleaning);

                // todo: at end of loop delete message, but not during debugging!
                currentMessage.setFlag(Flags.Flag.DELETED, true); // don't delete during debugging

            }


            // END LOOP


            inbox.close(true);
            store.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Return statement
        return cleanings;

    }


    // EXTRA FUNCTIONS


    private static Room findProperty(ArrayList<Room> roomList, String emailSnippet) {

        for (Room room : roomList) {

            if (emailSnippet.contains(room.getDescription())) {
                return room;
            }

        }

        // did not find room, return first room... gotta fix
        return roomList.get(0);

    }

}
