package org.launchcode.controllers;

import java.time.LocalDate;
import java.util.ArrayList;

public class LocalDateStuff {

    public String nothing() {
        // write your code here

        LocalDate today = LocalDate.now();

        LocalDate tomorrow = today.plusDays(1);

        ArrayList<LocalDate> dayList = new ArrayList<>();


        LocalDate abstractDate = today.plusDays(20);
        LocalDate moving = today.plusDays(0);

        for (int i = 1; i < 31; i++) {

            if (moving.equals(abstractDate)) {
                System.out.println("wow");
            }else {

                System.out.println(i + ": " + moving);
            }


            moving = moving.plusDays(1);
        }

        System.out.println(today.isBefore(tomorrow));
        System.out.println(tomorrow);

        LocalDate parsing = LocalDate.parse("2011-01-01");

        System.out.println(parsing);

        System.out.println(today.isEqual(parsing));

        return "";

    }
}
