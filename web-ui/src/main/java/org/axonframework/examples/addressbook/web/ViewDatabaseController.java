package org.axonframework.examples.addressbook.web;

import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.sample.app.command.ClaimedContactName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.nio.charset.Charset;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 * @author Jettro Coenradie
 */
@Controller
@RequestMapping(value = "/db")
public class ViewDatabaseController {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private EventStorageEngine eventStoreEngine;

    @RequestMapping(method = RequestMethod.GET)
    public String databaseQueries() {
        return "db/index";
    }

    @RequestMapping("/claimed")
    public String claimedNames(Model model) {
        @SuppressWarnings({"JpaQlInspection"})
        List<ClaimedContactName> claimedContactNames = entityManager.createQuery("select c from ClaimedContactName c")
                                                                    .getResultList();

        model.addAttribute("claimedNames", claimedContactNames);
        return "db/claimed";
    }

    @RequestMapping("/events")
    public String events(Model model) {
        Query nativeQuery = entityManager.createQuery(
                "select e.id,e.aggregateIdentifier,e.sequenceNumber,e.timeStamp,e.type,e.payload from DomainEventEntry e");
        List<Object[]> events = nativeQuery.getResultList();
        for (Object[] event : events) {
            event[5] = new String((byte[]) event[5], Charset.forName("UTF-8"));
        }
        model.addAttribute("events", events);
        return "db/events";
    }
}
