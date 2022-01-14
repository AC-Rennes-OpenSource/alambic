/*******************************************************************************
 * Copyright (C) 2019-2021 Rennes - Brittany Education Authority (<http://www.ac-rennes.fr>) and others.
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
package fr.gouv.education.acrennes.alambic.jobs.load.gar.builder;

import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.GARPersonProfils;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class GAR1DBuilderTestUtils {

    public <T> void assertListEquals(List<T> expected, List<T> actual, BiConsumer<List<T>, List<T>> assertMethod) {
        Assert.assertTrue((Objects.isNull(expected) && Objects.isNull(actual)) ||
                (expected != null && actual != null && expected.size() == actual.size()));
        if (expected != null && actual != null) {
            assertMethod.accept(expected, actual);
        }
    }

    public void assertStringListEquals(List<String> expected, List<String> actual) {
        List<String> workExpected = expected.stream().sorted().collect(Collectors.toList());
        List<String> workActual = actual.stream().sorted().collect(Collectors.toList());
        for (int i = 0; i < workExpected.size(); i++) {
            Assert.assertEquals(workExpected.get(i), workActual.get(i));
        }
    }

    public <T> boolean listsEquals(List<T> expected, List<T> actual, BiPredicate<List<T>, List<T>> testMethod) {
        if (Objects.isNull(expected) && Objects.isNull(actual)) {
            return true;
        } else if (Objects.isNull(expected) || Objects.isNull(actual) || expected.size() != actual.size()) {
            return false;
        } else {
            return testMethod.test(expected, actual);
        }
    }

    public boolean garProfilsEquals(List<GARPersonProfils> expected, List<GARPersonProfils> actual) {
        // Par construction on garantit que les listes sont non nulles et ont la même taille
        List<GARPersonProfils> workExpected = expected.stream().sorted(Comparator.comparing(GARPersonProfils::getGARStructureUAI)).collect(Collectors.toList());
        List<GARPersonProfils> workActual = actual.stream().sorted(Comparator.comparing(GARPersonProfils::getGARStructureUAI)).collect(Collectors.toList());
        boolean result = true;
        for (int i = 0; i < workExpected.size(); i++) {
            result = result
                    && StringUtils.equals(workExpected.get(i).getGARStructureUAI(), workActual.get(i).getGARStructureUAI())
                    && StringUtils.equals(workExpected.get(i).getGARPersonProfil(), workActual.get(i).getGARPersonProfil());
        }
        return result;
    }

    public boolean stringListsEquals(List<String> expected, List<String> actual) {
        // Par construction on garantit que les listes sont non nulles et ont la même taille
        List<String> workExpected = expected.stream().sorted().collect(Collectors.toList());
        List<String> workActual = actual.stream().sorted().collect(Collectors.toList());
        boolean result = true;
        for (int i = 0; i < workExpected.size(); i++) {
            result = result && StringUtils.equals(workExpected.get(i), workActual.get(i));
        }
        return result;
    }

}
