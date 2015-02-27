
package org.synyx.urlaubsverwaltung.web.person;

import org.joda.time.DateMidnight;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;

import org.springframework.validation.DataBinder;
import org.springframework.validation.Errors;

import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import org.synyx.urlaubsverwaltung.core.account.Account;
import org.synyx.urlaubsverwaltung.core.account.AccountService;
import org.synyx.urlaubsverwaltung.core.calendar.Day;
import org.synyx.urlaubsverwaltung.core.calendar.workingtime.WorkingTime;
import org.synyx.urlaubsverwaltung.core.calendar.workingtime.WorkingTimeService;
import org.synyx.urlaubsverwaltung.core.person.Person;
import org.synyx.urlaubsverwaltung.core.person.PersonInteractionService;
import org.synyx.urlaubsverwaltung.core.person.PersonService;
import org.synyx.urlaubsverwaltung.security.SessionService;
import org.synyx.urlaubsverwaltung.web.ControllerConstants;
import org.synyx.urlaubsverwaltung.web.DateMidnightPropertyEditor;
import org.synyx.urlaubsverwaltung.web.DecimalNumberPropertyEditor;
import org.synyx.urlaubsverwaltung.web.validator.PersonValidator;

import java.math.BigDecimal;

import java.util.Locale;


/**
 * @author  Aljona Murygina - murygina@synyx.de
 */
@Controller
public class PersonManagementController {

    @Autowired
    private PersonInteractionService personInteractionService;

    @Autowired
    private PersonService personService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PersonValidator validator;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private WorkingTimeService workingTimeService;

    @InitBinder
    public void initBinder(DataBinder binder, Locale locale) {

        binder.registerCustomEditor(DateMidnight.class, new DateMidnightPropertyEditor());
        binder.registerCustomEditor(BigDecimal.class, new DecimalNumberPropertyEditor(locale));
    }


    @RequestMapping(value = "/staff/new", method = RequestMethod.GET)
    public String newPersonForm(Model model) {

        if (sessionService.isOffice()) {
            Person person = new Person();

            PersonForm personForm = new PersonForm();
            addModelAttributesForPersonForm(person, personForm, model);

            return PersonConstants.PERSON_FORM_JSP;
        } else {
            return ControllerConstants.ERROR_JSP;
        }
    }


    @RequestMapping(value = "/staff", method = RequestMethod.POST)
    public String newPerson(@ModelAttribute("personForm") PersonForm personForm, Errors errors, Model model) {

        Person person = new Person();

        // validate login name
        validator.validateLogin(personForm.getLoginName(), errors);
        validator.validate(personForm, errors);

        if (errors.hasGlobalErrors()) {
            model.addAttribute("errors", errors);
        }

        if (errors.hasErrors()) {
            addModelAttributesForPersonForm(person, personForm, model);

            return PersonConstants.PERSON_FORM_JSP;
        }

        personInteractionService.createOrUpdate(person, personForm);

        return "redirect:/web/staff/";
    }


    @RequestMapping(value = "/staff/{personId}/edit", method = RequestMethod.GET)
    public String editPersonForm(@PathVariable("personId") Integer personId,
        @RequestParam(value = ControllerConstants.YEAR, required = false) Integer year, Model model) {

        int currentYear = DateMidnight.now().getYear();
        int yearOfHolidaysAccount;

        if (year != null) {
            yearOfHolidaysAccount = year;

            if (yearOfHolidaysAccount - currentYear > 2 || currentYear - yearOfHolidaysAccount > 2) {
                return ControllerConstants.ERROR_JSP;
            }
        } else {
            yearOfHolidaysAccount = currentYear;
        }

        if (sessionService.isOffice()) {
            Person person = personService.getPersonByID(personId);

            PersonForm personForm = preparePersonForm(yearOfHolidaysAccount, person);
            addModelAttributesForPersonForm(person, personForm, model);

            return PersonConstants.PERSON_FORM_JSP;
        } else {
            return ControllerConstants.ERROR_JSP;
        }
    }


    private PersonForm preparePersonForm(int year, Person person) {

        Account account = accountService.getHolidaysAccount(year, person);

        WorkingTime workingTime = workingTimeService.getCurrentOne(person);

        return new PersonForm(person, String.valueOf(year), account, workingTime, person.getPermissions(),
                person.getNotifications());
    }


    private void addModelAttributesForPersonForm(Person person, PersonForm personForm, Model model) {

        model.addAttribute(PersonConstants.LOGGED_USER, sessionService.getLoggedUser());
        model.addAttribute(ControllerConstants.PERSON, person);
        model.addAttribute("personForm", personForm);
        model.addAttribute("currentYear", DateMidnight.now().getYear());
        model.addAttribute("weekDays", Day.values());

        if (person.getId() != null) {
            model.addAttribute("workingTimes", workingTimeService.getByPerson(person));
        }
    }


    @RequestMapping(value = "/staff/{personId}/edit", method = RequestMethod.PUT)
    public String editPerson(@PathVariable("personId") Integer personId,
        @ModelAttribute("personForm") PersonForm personForm, Errors errors, Model model) {

        Person personToUpdate = personService.getPersonByID(personId);

        validator.validate(personForm, errors);

        if (errors.hasGlobalErrors()) {
            model.addAttribute("errors", errors);
        }

        if (errors.hasErrors()) {
            addModelAttributesForPersonForm(personToUpdate, personForm, model);

            return PersonConstants.PERSON_FORM_JSP;
        }

        personInteractionService.createOrUpdate(personToUpdate, personForm);

        return "redirect:/web/staff/";
    }
}
