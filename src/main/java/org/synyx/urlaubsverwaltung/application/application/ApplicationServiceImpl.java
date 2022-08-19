package org.synyx.urlaubsverwaltung.application.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.synyx.urlaubsverwaltung.absence.DateRange;
import org.synyx.urlaubsverwaltung.application.vacationtype.VacationCategory;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.person.PersonDeletedEvent;
import org.synyx.urlaubsverwaltung.util.DecimalConverter;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.math.RoundingMode.HALF_EVEN;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationStatus.ALLOWED;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationStatus.ALLOWED_CANCELLATION_REQUESTED;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationStatus.TEMPORARY_ALLOWED;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationStatus.WAITING;
import static org.synyx.urlaubsverwaltung.application.vacationtype.VacationCategory.OVERTIME;
import static org.synyx.urlaubsverwaltung.util.DecimalConverter.toFormattedDecimal;

/**
 * Implementation of interface {@link ApplicationService}.
 */
@Service
class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;

    @Autowired
    ApplicationServiceImpl(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @Override
    public Optional<Application> getApplicationById(Integer id) {
        return applicationRepository.findById(id);
    }

    @Override
    public Application save(Application application) {
        return applicationRepository.save(application);
    }

    @Override
    public List<Application> getApplicationsForACertainPeriodAndPerson(LocalDate startDate, LocalDate endDate, Person person) {
        return applicationRepository.getApplicationsForACertainTimeAndPerson(startDate, endDate, person);
    }

    @Override
    public List<Application> getApplicationsForACertainPeriod(LocalDate startDate, LocalDate endDate, List<Person> persons) {
        return applicationRepository.findByPersonInAndEndDateIsGreaterThanEqualAndStartDateIsLessThanEqual(persons, startDate, endDate);
    }

    @Override
    public List<Application> getApplicationsStartingInACertainPeriodAndPersonAndVacationCategory(LocalDate startDate, LocalDate endDate, Person person, List<ApplicationStatus> statuses, VacationCategory vacationCategory) {
        return applicationRepository.findByStatusInAndPersonAndStartDateBetweenAndVacationTypeCategory(statuses, person, startDate, endDate, vacationCategory);
    }

    @Override
    public List<Application> getApplicationsForACertainPeriodAndPersonAndVacationCategory(LocalDate startDate, LocalDate endDate, Person person, List<ApplicationStatus> statuses, VacationCategory vacationCategory) {
        return applicationRepository.findByStatusInAndPersonAndEndDateIsGreaterThanEqualAndStartDateIsLessThanEqualAndVacationTypeCategory(statuses, person, startDate, endDate, vacationCategory);
    }

    @Override
    public List<Application> getApplicationsForACertainPeriodAndState(LocalDate startDate, LocalDate endDate, ApplicationStatus status) {
        return applicationRepository.getApplicationsForACertainTimeAndState(startDate, endDate, status);
    }

    @Override
    public List<Application> getApplicationsWhereApplicantShouldBeNotifiedAboutUpcomingApplication(LocalDate from, LocalDate to, List<ApplicationStatus> statuses) {
        return applicationRepository.findByStatusInAndStartDateBetweenAndUpcomingApplicationsReminderSendIsNull(statuses, from, to);
    }

    @Override
    public List<Application> getApplicationsWhereHolidayReplacementShouldBeNotified(LocalDate from, LocalDate to, List<ApplicationStatus> statuses) {
        return applicationRepository.findByStatusInAndStartDateBetweenAndHolidayReplacementsIsNotEmptyAndUpcomingHolidayReplacementNotificationSendIsNull(statuses, from, to);
    }

    @Override
    public List<Application> getApplicationsForACertainPeriodAndPersonAndState(LocalDate startDate, LocalDate endDate, Person person, ApplicationStatus status) {
        return applicationRepository.getApplicationsForACertainTimeAndPersonAndState(startDate, endDate, person, status);
    }

    @Override
    public List<Application> getForStates(List<ApplicationStatus> statuses) {
        return applicationRepository.findByStatusIn(statuses);
    }

    @Override
    public List<Application> getForStatesSince(List<ApplicationStatus> statuses, LocalDate since) {
        return applicationRepository.findByStatusInAndEndDateGreaterThanEqual(statuses, since);
    }

    @Override
    public List<Application> getForStatesAndPerson(List<ApplicationStatus> statuses, List<Person> persons) {
        return applicationRepository.findByStatusInAndPersonIn(statuses, persons);
    }

    @Override
    public List<Application> getForStatesAndPersonSince(List<ApplicationStatus> statuses, List<Person> persons, LocalDate since) {
        return applicationRepository.findByStatusInAndPersonInAndEndDateIsGreaterThanEqual(statuses, persons, since);
    }

    @Override
    public List<Application> getForStatesAndPerson(List<ApplicationStatus> statuses, List<Person> persons, LocalDate start, LocalDate end) {
        return applicationRepository.findByStatusInAndPersonInAndEndDateIsGreaterThanEqualAndStartDateIsLessThanEqual(statuses, persons, start, end);
    }

    @Override
    public Duration getTotalOvertimeReductionOfPerson(Person person) {
        final BigDecimal overtimeReduction = Optional.ofNullable(applicationRepository.calculateTotalOvertimeReductionOfPerson(person)).orElse(BigDecimal.ZERO);
        return Duration.ofMinutes(overtimeReduction.multiply(BigDecimal.valueOf(60)).longValue());
    }

    @Override
    public Duration getTotalOvertimeReductionOfPerson(Person person, LocalDate start, LocalDate end) {

        final DateRange dateRangeOfPeriod = new DateRange(start, end);

        final List<ApplicationStatus> waitingAndAllowedStatus = List.of(WAITING, TEMPORARY_ALLOWED, ALLOWED, ALLOWED_CANCELLATION_REQUESTED);
        return applicationRepository.findByPersonAndVacationTypeCategoryAndStatusInAndEndDateIsGreaterThanEqualAndStartDateIsLessThanEqual(person, OVERTIME, waitingAndAllowedStatus, start, end).stream()
            .map(application -> {
                final DateRange applicationDateRage = new DateRange(application.getStartDate(), application.getEndDate());
                final Duration durationOfOverlap = dateRangeOfPeriod.overlap(applicationDateRage).map(DateRange::duration).orElse(Duration.ZERO);
                return toFormattedDecimal(application.getHours())
                    .divide(toFormattedDecimal(applicationDateRage.duration()), HALF_EVEN)
                    .multiply(toFormattedDecimal(durationOfOverlap)).setScale(0, HALF_EVEN);
            })
            .map(DecimalConverter::toDuration)
            .reduce(Duration.ZERO, Duration::plus);
    }

    @Override
    public Duration getTotalOvertimeReductionOfPersonBefore(Person person, LocalDate date) {
        final BigDecimal overtimeReduction = Optional.ofNullable(applicationRepository.calculateTotalOvertimeReductionOfPersonBefore(person, date)).orElse(BigDecimal.ZERO);
        return Duration.ofMinutes(overtimeReduction.multiply(BigDecimal.valueOf(60)).longValue());
    }

    @Override
    public Map<Person, Duration> getTotalOvertimeReductionOfPersonsBefore(Collection<Person> persons, LocalDate date) {
        final Map<Person, Duration> durationByPerson = applicationRepository.calculateTotalOvertimeReductionOfPersonsBefore(persons, date).stream()
            .map(sum -> {
                final BigDecimal minutes = BigDecimal.valueOf(sum.getDurationDouble()).multiply(BigDecimal.valueOf(60));
                final Duration duration = Duration.ofMinutes(minutes.longValue());
                return Map.entry(sum.getPerson(), duration);
            })
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        // iterate over given persons to create Duration.ZERO entries for persons without overtime reduction applications.
        return persons.stream()
            .map(person -> Map.entry(person, durationByPerson.getOrDefault(person, Duration.ZERO)))
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public List<Application> getForHolidayReplacement(Person holidayReplacement, LocalDate date) {
        final List<ApplicationStatus> status = List.of(WAITING, TEMPORARY_ALLOWED, ALLOWED, ALLOWED_CANCELLATION_REQUESTED);
        return applicationRepository.findByHolidayReplacements_PersonAndEndDateIsGreaterThanEqualAndStatusIn(holidayReplacement, date, status);
    }

    @Override
    public List<Application> deleteApplicationsByPerson(Person person) {
        return applicationRepository.deleteByPerson(person);
    }

    @Override
    public void deleteInteractionWithApplications(Person person) {
        final List<Application> applicationsWithoutBoss = applicationRepository.findByBoss(person);
        applicationsWithoutBoss.forEach(application -> application.setBoss(null));
        applicationRepository.saveAll(applicationsWithoutBoss);

        final List<Application> applicationsWithoutCanceller = applicationRepository.findByCanceller(person);
        applicationsWithoutCanceller.forEach(application -> application.setCanceller(null));
        applicationRepository.saveAll(applicationsWithoutCanceller);

        final List<Application> applicationsWithoutApplier = applicationRepository.findByApplier(person);
        applicationsWithoutApplier.forEach(application -> application.setApplier(null));
        applicationRepository.saveAll(applicationsWithoutApplier);
    }

    /**
     * Deletes all application replacements of applications.
     *
     * @param event the person which is deleted
     */
    @EventListener
    void deleteHolidayReplacements(PersonDeletedEvent event) {
        final List<Application> applicationsWithReplacedApplicationReplacements = applicationRepository.findAllByHolidayReplacements_Person(event.getPerson()).stream()
            .map(deleteHolidayReplacement(event.getPerson()))
            .collect(toList());
        applicationRepository.saveAll(applicationsWithReplacedApplicationReplacements);
    }

    private Function<Application, Application> deleteHolidayReplacement(Person deletedPerson) {
        return application -> {
            application.setHolidayReplacements(application.getHolidayReplacements().stream()
                .filter(holidayReplacementEntity -> !holidayReplacementEntity.getPerson().equals(deletedPerson))
                .collect(toList())
            );

            return application;
        };
    }
}
