package org.synyx.urlaubsverwaltung.application.statistics;

import org.synyx.urlaubsverwaltung.application.vacationtype.VacationType;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.person.basedata.PersonBasedata;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.math.BigDecimal.ZERO;

/**
 * Encapsulates information about a person and the corresponding vacation days information (how many applications for
 * leave are waiting, how many are allowed, how many vacation days has the person left for using).
 */
public class ApplicationForLeaveStatistics {

    private final Person person;
    private PersonBasedata personBasedata;

    private final Map<VacationType, BigDecimal> waitingVacationDays = new HashMap<>();
    private final Map<VacationType, BigDecimal> allowedVacationDays = new HashMap<>();

    private BigDecimal leftVacationDays = ZERO;
    private BigDecimal leftRemainingVacationDays = ZERO;
    private BigDecimal leftVacationDaysForPeriod = ZERO;
    private BigDecimal leftRemainingVacationDaysForPeriod = ZERO;
    private Duration leftOvertime = Duration.ZERO;

    ApplicationForLeaveStatistics(Person person) {
        this.person = person;
    }

    public Person getPerson() {
        return person;
    }

    void setPersonBasedata(PersonBasedata personBasedata) {
        this.personBasedata = personBasedata;
    }

    public Optional<PersonBasedata> getPersonBasedata() {
        return Optional.ofNullable(personBasedata);
    }

    public Map<VacationType, BigDecimal> getWaitingVacationDays() {
        return waitingVacationDays;
    }

    public Map<VacationType, BigDecimal> getAllowedVacationDays() {
        return allowedVacationDays;
    }

    public BigDecimal getLeftVacationDays() {
        return leftVacationDays;
    }

    public void setLeftVacationDays(BigDecimal leftVacationDays) {
        this.leftVacationDays = leftVacationDays;
    }

    public BigDecimal getLeftRemainingVacationDays() {
        return leftRemainingVacationDays;
    }

    public void setLeftRemainingVacationDays(BigDecimal leftRemainingVacationDays) {
        this.leftRemainingVacationDays = leftRemainingVacationDays;
    }

    public BigDecimal getLeftVacationDaysForPeriod() {
        return leftVacationDaysForPeriod;
    }

    public void setLeftVacationDaysForPeriod(BigDecimal leftVacationDaysForPeriod) {
        this.leftVacationDaysForPeriod = leftVacationDaysForPeriod;
    }

    public BigDecimal getLeftRemainingVacationDaysForPeriod() {
        return leftRemainingVacationDaysForPeriod;
    }

    public void setLeftRemainingVacationDaysForPeriod(BigDecimal leftRemainingVacationDaysForPeriod) {
        this.leftRemainingVacationDaysForPeriod = leftRemainingVacationDaysForPeriod;
    }

    public Duration getLeftOvertime() {
        return leftOvertime;
    }

    public void setLeftOvertime(Duration leftOvertime) {
        this.leftOvertime = leftOvertime;
    }

    public BigDecimal getTotalWaitingVacationDays() {
        return getWaitingVacationDays().values().stream()
            .reduce(ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalAllowedVacationDays() {
        return getAllowedVacationDays().values().stream()
            .reduce(ZERO, BigDecimal::add);
    }

    public void addWaitingVacationDays(VacationType vacationType, BigDecimal waitingVacationDays) {
        final BigDecimal currentWaitingVacationDays = getWaitingVacationDays().getOrDefault(vacationType, ZERO);
        getWaitingVacationDays().put(vacationType, currentWaitingVacationDays.add(waitingVacationDays));
    }

    public void addAllowedVacationDays(VacationType vacationType, BigDecimal allowedVacationDays) {
        final BigDecimal currentAllowedVacationDays = getAllowedVacationDays().getOrDefault(vacationType, ZERO);
        getAllowedVacationDays().put(vacationType, currentAllowedVacationDays.add(allowedVacationDays));
    }

    public boolean hasVacationType(VacationType type) {
        return waitingVacationDays.containsKey(type) || allowedVacationDays.containsKey(type);
    }

    public BigDecimal getWaitingVacationDays(VacationType type) {
        return waitingVacationDays.getOrDefault(type, ZERO);
    }

    public BigDecimal getAllowedVacationDays(VacationType type) {
        return allowedVacationDays.getOrDefault(type, ZERO);
    }
}
