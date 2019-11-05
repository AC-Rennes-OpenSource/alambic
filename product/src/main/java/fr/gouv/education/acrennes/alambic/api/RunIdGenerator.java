/*******************************************************************************
 * Copyright (C) 2019 Rennes - Brittany Education Authority (<http://www.ac-rennes.fr>) and others.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package fr.gouv.education.acrennes.alambic.api;

import java.time.*;
import java.time.temporal.ChronoField;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Générateur d'identifiant d'exécution. Une même instance peut et doit être utilisée dans un environnement
 * multithreadé, afin de minimiser le risque de collision d'identifiant.
 * <p>
 * La valeur générée par ce composant a le format {@code DATE-HEURE+MS-COMPTEUR}. Exemple de valeur possible : {@code
 * 20190206-180422412-0045}
 * </p>
 */
public final class RunIdGenerator {

    private static final int FIRST_COUNTER_VALUE = 0;
    private static final int COUNTER_DIGITS = 4;
    private static final int MAX_COUNTER_VALUE = (int) Math.pow(16, COUNTER_DIGITS) - 1;

    private final AtomicInteger counter;
    private final Clock clock;

    /**
     * Constructeur.
     */
    public RunIdGenerator() {
        this.counter = new AtomicInteger(FIRST_COUNTER_VALUE);
        this.clock = Clock.systemUTC();
    }

    /**
     * Constructeur alternatif pour les tests. Permet d'initialiser l'état du générateur de telle sorte à disposer d'un
     * comportement prédictible.
     *
     * @param initCounter la valeur à utiliser pour initialiser le compteur utilisé pour la génération de l'identifiant
     * @param clock       l'horloge utilisée lors de la détermination de la date et de l'heure utilisés pour générer
     *                    l'identifiant ; en situation de test, on souhaitera utiliser une horloge construite à l'aide
     *                    de {@link Clock#fixed(Instant, ZoneId)}
     */
    RunIdGenerator(final int initCounter, final Clock clock) {
        assert clock != null;
        this.counter = new AtomicInteger(initCounter);
        this.clock = clock;
    }

    public String nextId() {
        final LocalDateTime dateTime = LocalDateTime.now(clock);

        final String datePart = buildDatePart(dateTime.toLocalDate());
        final String timePart = buildTimePart(dateTime.toLocalTime());
        final String counterPart = buildCounterPart();

        return datePart + "-" + timePart + "-" + counterPart;
    }

    private String buildDatePart(final LocalDate datedate) {
        return String.format(
                "%d%02d%02d",
                datedate.getYear(),
                datedate.getMonthValue(),
                datedate.getDayOfMonth()
        );
    }

    private String buildTimePart(final LocalTime time) {
        return String.format(
                "%02d%02d%02d%03d",
                time.getHour(),
                time.getMinute(),
                time.getSecond(),
                time.get(ChronoField.MILLI_OF_SECOND)
        );
    }

    private String buildCounterPart() {
        return String.format(
                "%0" + COUNTER_DIGITS + "X",
                counter.updateAndGet(value -> (value >= MAX_COUNTER_VALUE) ? FIRST_COUNTER_VALUE : value + 1)
        );
    }
}
