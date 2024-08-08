package org.synyx.urlaubsverwaltung.publicholiday;

import de.focus_shift.jollyday.core.HolidayManager;
import de.focus_shift.jollyday.core.ManagerParameters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
class PublicHolidayConfiguration {

    private static final List<String> COUNTRIES = List.of("de", "at", "ch", "gb", "gr", "mt", "it", "hr", "es", "nl", "lt");

    @Bean
    Map<String, HolidayManager> holidayManagerMap() {
        final Map<String, HolidayManager> countryMap = new HashMap<>();
        COUNTRIES.forEach(country -> {
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            final URL url = cl.getResource("Holidays_" + country + ".xml");
            countryMap.put(country, HolidayManager.getInstance(ManagerParameters.create(url)));
        });
        return countryMap;
    }
}
