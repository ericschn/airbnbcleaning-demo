package org.launchcode.controllers;

import org.launchcode.models.Cleaning;
import org.launchcode.models.Room;
import org.launchcode.models.data.CleaningDao;
import org.launchcode.models.data.RoomDao;
import org.launchcode.controllers.GetMail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;


import javax.validation.Valid;
import java.sql.Array;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
public class CleaningController {

    @Autowired
    private CleaningDao cleaningDao;

    @Autowired
    private RoomDao roomDao;

    // TESTING STATIC STUFF HERE

    private static ArrayList<ArrayList<Cleaning>> staticWeekList = new ArrayList<>();
    private static LocalDate weekStamp = LocalDate.now().minusMonths(3);

    // TODO: create static room list

    @RequestMapping("")
    public String index(Model model) {

        // Populate roomList
        ArrayList<Room> roomList = (ArrayList<Room>) roomDao.findAll();

        // CHECK EMAIL - need to put this on every page
        ArrayList<Cleaning> newCleanings = GetMail.run(roomList);

        // Add new cleanings found in email to database
        for (Cleaning newCleaning : newCleanings) {

            // todo: check for duplicate cleanings

           List<Cleaning> checkCleanings = cleaningDao.findByDateAndRoom(newCleaning.getDate(), newCleaning.getRoom());

           if (!checkCleanings.isEmpty()) {
               System.out.println("Duplicate found, skipping adding this cleaning");
               continue;
           }

            newCleaning.setNotes(""); // set notes as blank string

            System.out.println("Successfully added new cleaning!");
            cleaningDao.save(newCleaning);
        }

        LocalDate todayLocal = LocalDate.now(ZoneId.of("GMT-07:00"));
        LocalDate cleanLocal;

        // Create list of dates to populate headings in week view
        ArrayList<String> weekDates = new ArrayList<>();

        ArrayList<ArrayList<Cleaning>> weekList = new ArrayList<>();

        // Checking if week view is out of date
        if (weekStamp.isEqual(todayLocal)) {

            System.out.println("Static week list is up to date"); // DEBUGGING

            for (int i = 0; i < 7; i++) {

                // add date string to weekDates list
                weekDates.add(todayLocal.plusDays(i).toString());
            }

            weekList = staticWeekList;

        }else{

            System.out.println("Static week list is out of date, updating..."); // DEBUGGING

            weekStamp = todayLocal;

            // Grab cleanings for this week
            Date searchFrom = Date.valueOf(todayLocal.minusDays(1));
            Date searchTo = Date.valueOf(todayLocal.plusDays(7));

            // Query database for all cleanings in the month
            ArrayList<Cleaning> cleanings = (ArrayList<Cleaning>) cleaningDao.findByDateBetween(searchFrom, searchTo);


            // building the week view
            for (int i = 0; i < 7; i++) {
                ArrayList<Cleaning> dayList = new ArrayList<>();  // initialize new ArrayList every day

                // add date string to weekDates list
                weekDates.add(todayLocal.plusDays(i).toString());

                for (Cleaning cleaning : cleanings) {
                    cleanLocal = cleaning.getDate().toLocalDate();

                    // if cleaning date matches the date of current iteration, using var i
                    if (cleanLocal.isEqual(todayLocal.plusDays(i))) {
                        dayList.add(cleaning);
                    }
                }
                weekList.add(dayList);
            }

            // assign to the static variable
            staticWeekList = weekList;
        }


        // creator of the new time api calls this a horrible hack...
        Date today = Date.valueOf(LocalDate.now(ZoneId.of("GMT-07:00")));

        model.addAttribute("today", today);
        model.addAttribute("weekList", weekList);
        model.addAttribute("weekDates", weekDates);

        return "index";
    }

    @RequestMapping(value = "/month", method = RequestMethod.GET)
    public String month(Model model, @RequestParam(value = "m", required = false) String m) {

        // todo: replace all localdate.now() calls to a variable created here

        // Set timezone
        ZoneId zone = ZoneId.of("GMT-07:00");

        // Sets default value as the first day of this month
        LocalDate monthToViewLocal = LocalDate.now(zone).minusDays(LocalDate.now(zone).getDayOfMonth()-1);

        // Find out what month we're trying to view via get params

        if (m != null) {

            // TODO: validate m

            try {
                Date monthToView = Date.valueOf(m);
                monthToViewLocal = monthToView.toLocalDate().minusDays(monthToView.toLocalDate().getDayOfMonth()-1);
            } catch (Exception e){
                monthToViewLocal = LocalDate.now(zone).minusDays(LocalDate.now(zone).getDayOfMonth()-1);
            }
        }

        // Initialize month to month nav links
        String prevMonth = monthToViewLocal.minusMonths(1).toString();
        String nextMonth = monthToViewLocal.plusMonths(1).toString();

        // Search params go from last day of previous month to first day of next month
        Date searchFrom = Date.valueOf(monthToViewLocal.minusDays(1));
        Date searchTo = Date.valueOf(monthToViewLocal.plusMonths(1));

        // Query database for all cleanings in the month
        ArrayList<Cleaning> cleanings = (ArrayList<Cleaning>) cleaningDao.findByDateBetween(searchFrom, searchTo);

        // Create list of rooms to populate table headers
        ArrayList<Room> roomList = (ArrayList<Room>) roomDao.findAll();

        // Create list for date objects of this month
        ArrayList<ArrayList<Cleaning>> monthList = new ArrayList<>();

        // Create list of the prices per day
        ArrayList<Integer> priceList = new ArrayList<>();

        String monthString = monthToViewLocal.getMonth().toString().substring(0,1) +
                monthToViewLocal.getMonth().toString().substring(1).toLowerCase() +
                " " + monthToViewLocal.getYear(); // get the month as String - this one's so dumb

        // loop through month and add day to dayList
        LocalDate cleanLocal;
        int monthValue = monthToViewLocal.getMonthValue();
        while (monthToViewLocal.getMonthValue() == monthValue) {

            ArrayList<Cleaning> dayList = new ArrayList<>();

            for (Cleaning cleaning : cleanings) {
                cleanLocal = cleaning.getDate().toLocalDate();

                // if cleaning date matches the date of current iteration, using var i
                if (cleanLocal.isEqual(monthToViewLocal)) {
                    dayList.add(cleaning);
                }
            }

            // add total price to price list
            int addPrice = 0;
            for (Cleaning cleaning : dayList) {
                addPrice += cleaning.getRoom().getPrice();
            }
            priceList.add(addPrice);

            monthList.add(dayList);
            monthToViewLocal = monthToViewLocal.plusDays(1);
        }

        // grand total price
        int totalPrice = priceList.stream().mapToInt(Integer::intValue).sum();


        model.addAttribute("roomList", roomList);
        model.addAttribute("monthList", monthList);
        model.addAttribute("priceList", priceList);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("monthString", monthString);
        // nav links
        model.addAttribute("prevMonth", prevMonth);
        model.addAttribute("nextMonth", nextMonth);

        return "month";

    }

    @RequestMapping(value = "/admin")
    public String admin(Model model, @RequestParam(value = "m", required = false) String m) {

        // todo: replace all localdate.now() calls to a variable created here

        // Set timezone
        ZoneId zone = ZoneId.of("GMT-07:00");

        // Sets default value as the first day of this month
        LocalDate monthToViewLocal = LocalDate.now(zone).minusDays(LocalDate.now(zone).getDayOfMonth()-1);

        // Find out what month we're trying to view via get params

        if (m != null) {

            // TODO: validate m

            try {
                Date monthToView = Date.valueOf(m);
                monthToViewLocal = monthToView.toLocalDate().minusDays(monthToView.toLocalDate().getDayOfMonth()-1);
            } catch (Exception e){
                monthToViewLocal = LocalDate.now(zone).minusDays(LocalDate.now(zone).getDayOfMonth()-1);
            }
        }

        // Initialize month to month nav links
        String prevMonth = monthToViewLocal.minusMonths(1).toString();
        String nextMonth = monthToViewLocal.plusMonths(1).toString();

        // Search params go from last day of previous month to first day of next month
        Date searchFrom = Date.valueOf(monthToViewLocal.minusDays(1));
        Date searchTo = Date.valueOf(monthToViewLocal.plusMonths(1));

        // Query database for all cleanings in the month
        ArrayList<Cleaning> cleanings = (ArrayList<Cleaning>) cleaningDao.findByDateBetween(searchFrom, searchTo);

        // Create list of rooms to populate table headers
        ArrayList<Room> roomList = (ArrayList<Room>) roomDao.findAll();

        // Create list for date objects of this month
        ArrayList<ArrayList<Cleaning>> monthList = new ArrayList<>();

        // Create list of the prices per day
        ArrayList<Integer> priceList = new ArrayList<>();

        String monthString = monthToViewLocal.getMonth().toString().substring(0,1) +
                monthToViewLocal.getMonth().toString().substring(1).toLowerCase() +
                " " + monthToViewLocal.getYear(); // get the month as String - this one's so dumb

        // loop through month and add day to dayList
        LocalDate cleanLocal;
        int monthValue = monthToViewLocal.getMonthValue();
        while (monthToViewLocal.getMonthValue() == monthValue) {

            ArrayList<Cleaning> dayList = new ArrayList<>();

            for (Cleaning cleaning : cleanings) {
                cleanLocal = cleaning.getDate().toLocalDate();

                // if cleaning date matches the date of current iteration, using var i
                if (cleanLocal.isEqual(monthToViewLocal)) {
                    dayList.add(cleaning);
                }
            }

            // add total price to price list
            int addPrice = 0;
            for (Cleaning cleaning : dayList) {
                addPrice += cleaning.getRoom().getPrice();
            }
            priceList.add(addPrice);

            monthList.add(dayList);
            monthToViewLocal = monthToViewLocal.plusDays(1);
        }

        // grand total price
        int totalPrice = priceList.stream().mapToInt(Integer::intValue).sum();


        model.addAttribute("roomList", roomList);
        model.addAttribute("monthList", monthList);
        model.addAttribute("priceList", priceList);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("monthString", monthString);
        // nav links
        model.addAttribute("prevMonth", prevMonth);
        model.addAttribute("nextMonth", nextMonth);

        return "admin";

    }

    @RequestMapping(value = "/add", method = RequestMethod.GET)
    public String displayAddCleaningForm(Model model) {

        ArrayList<Room> roomList = (ArrayList<Room>) roomDao.findAll();
        model.addAttribute("roomList", roomList);
        model.addAttribute("cleaning", new Cleaning());
        return "add";

    }

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public String processAddCleaningForm(@ModelAttribute @Valid Cleaning newCleaning,
                                         Errors errors, @RequestParam int roomId,
                                         Model model) {

        if (errors.hasErrors()) {
            model.addAttribute("title", "Add Cleaning");
            ArrayList<Room> roomList = (ArrayList<Room>) roomDao.findAll(); //todo static room list
            model.addAttribute("roomList", roomList);
            return "add";
        }

        // TODO: Validate date further
        // Date must be greater than 01-01-2016 and less than (current year + 1)

        Room room = roomDao.findOne(roomId);

        newCleaning.setRoom(room);

        newCleaning.setNotes(newCleaning.getNotes().trim()); // trims off spaces from beg and end

        List<Cleaning> checkCleanings = cleaningDao.findByDateAndRoom(newCleaning.getDate(), newCleaning.getRoom());

        // Duplicate check
        if (checkCleanings.isEmpty()) {
            cleaningDao.save(newCleaning);

            // Make sure static week list updates by putting weekStamp out of date
            weekStamp = LocalDate.now().minusMonths(3);

        }else{
            model.addAttribute("error", "Cleaning already exists, click to edit it");
            model.addAttribute("dupCleaning", checkCleanings.get(0).getId());

            return "add";
        }

        return "redirect:/add";

    }

    @RequestMapping(value = "/edit", method = RequestMethod.GET)
    public String edit(Model model, @RequestParam(value = "id", required = true) int id) {

        Cleaning editCleaning = cleaningDao.findOne(id);

        ArrayList<Room> roomList = (ArrayList<Room>) roomDao.findAll(); // todo room list static
        model.addAttribute("roomList", roomList);
        model.addAttribute("editCleaning", editCleaning);

        return "edit";

    }


    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    public String processEdit(@ModelAttribute @Valid Cleaning editCleaning,
                                         Errors errors, @RequestParam int roomId,
                                         @RequestParam int id,
                                         Model model) {

        if (errors.hasErrors()) {
            model.addAttribute("title", "Add Cleaning");
            ArrayList<Room> roomList = (ArrayList<Room>) roomDao.findAll(); //todo static room list
            model.addAttribute("roomList", roomList);
            return "add";
        }



        // TODO: Validate date further
        // Date must be greater than 01-01-2016 and less than (current year + 1)

        // todo: make sure no duplicates

        Room room = roomDao.findOne(roomId);

        editCleaning.setRoom(room);

        cleaningDao.delete(id);
        cleaningDao.save(editCleaning);

        // Make sure static week list updates by putting weekStamp out of date
        weekStamp = LocalDate.now().minusMonths(3);

        return "redirect:/admin";

    }

    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public String delete(@RequestParam int id, Model model) {

        cleaningDao.delete(id);  // Remove from database

        // Make sure static week list updates by putting weekStamp out of date
        weekStamp = LocalDate.now().minusMonths(3);

        return "redirect:/admin";

    }



    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String loginShow(Model model) {

        return "login";

    }

}
