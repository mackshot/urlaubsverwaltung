package org.synyx.urlaubsverwaltung.service;

import org.joda.time.DateMidnight;

import org.synyx.urlaubsverwaltung.domain.Antrag;
import org.synyx.urlaubsverwaltung.domain.AntragStatus;
import org.synyx.urlaubsverwaltung.domain.Person;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import java.util.List;


/**
 * use this service to access to the request-data (who, how many days, ...)
 *
 * @author  johannes
 */
public interface AntragService {

    /**
     * use this to save or edit an existing request
     *
     * @param  antrag  the new version of the request
     */
    void save(Antrag antrag);


    /**
     * use this to set a request to approved (only boss)
     *
     * @param  antrag  the request to be edited
     */
    void approve(Antrag antrag);


    /**
     * use this to set a request to declined (only boss)
     *
     * @param  antrag  the request to be edited
     * @param  reason  the reason of the rejection
     */
    void decline(Antrag antrag, Person boss, String reasonToDecline);


    /**
     * if a user requests vacation, 'antrag' has state 'wartend' until a boss approves it
     *
     * @param  antrag
     */
    void wait(Antrag antrag);


    /**
     * state is set to 'storniert' if user cancels vacation
     *
     * @param  antrag
     */
    void storno(Antrag antrag);


    /**
     * Krankheitstage zum Antrag hinzufuegen: Urlaubskonto gutschreiben und von verbrauchten Urlaubstagen abziehen
     *
     * @param  antrag
     */
    void krankheitBeachten(Antrag antrag, Double krankheitsTage);


    /**
     * use this to get a certain request if you know its id
     *
     * @param  id  the id of the request
     *
     * @return  returns the request as an Antrag-object
     */
    Antrag getRequestById(Integer id);


    /**
     * use this to get all requests of a certain person
     *
     * @param  person  the person you want to get the requests of
     *
     * @return  returns all requests of a person as a list of Antrag-objects
     */
    List<Antrag> getAllRequestsForPerson(Person person);


    /**
     * use this to get all requests of the system
     *
     * @return  returns all requests of the system as a list of antrag-objects
     */
    List<Antrag> getAllRequests();


    /**
     * use this to get all requests of a certain state (like 'waiting')
     *
     * @return  returns all requests of a state as a list of antrag-objects
     */
    List<Antrag> getAllRequestsByState(AntragStatus state);


    /**
     * use this to get all requests with vacation time between startDate x and endDate y
     *
     * @param  startDate
     * @param  endDate
     *
     * @return
     */
    List<Antrag> getAllRequestsForACertainTime(DateMidnight startDate, DateMidnight endDate);


    /**
     * signs a request with private key of the input person
     *
     * @param  antrag
     * @param  signierendePerson
     */
    void signAntrag(Antrag antrag, Person signierendePerson, boolean isBoss) throws NoSuchAlgorithmException,
        InvalidKeySpecException;


    /**
     * ueberprueft, ob antrag wirklich gestellt werden kann oder ob gewuenschter urlaubszeitraum das urlaubskonto des
     * antragstellers sprengt.
     *
     * @param  antrag
     *
     * @return
     */
    boolean checkAntrag(Antrag antrag);


    boolean checkAntragOneYear(Antrag antrag, DateMidnight start, DateMidnight end);


    boolean checkAntragTwoYears(Antrag antrag, DateMidnight start, DateMidnight end);
}
