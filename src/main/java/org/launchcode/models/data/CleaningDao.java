package org.launchcode.models.data;

import org.launchcode.models.Cleaning;
import org.launchcode.models.Room;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;

@Repository
@Transactional
public interface CleaningDao extends CrudRepository<Cleaning, Integer>{

    List<Cleaning> findByDate(Date date);

    List<Cleaning> findByDateAndRoom(Date date, Room room);

    List<Cleaning> findByDateAfter(Date date);

    List<Cleaning> findByDateBetween(Date dateStart, Date dateEnd);

}
