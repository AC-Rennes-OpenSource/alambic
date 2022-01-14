/*******************************************************************************
 * Copyright (C) 2019-2020 Rennes - Brittany Education Authority (<http://www.ac-rennes.fr>) and others.
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
package fr.gouv.education.acrennes.alambic.jobs.extract.clients;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class WSToStateBaseGetFullAPIURL {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {
                        "http://localhost", "/api?", "test",
                        "http://localhost/api?test"
                },
                {
                        "http://localhost:8080", "/api/", "clef=valeur",
                        "http://localhost:8080/api/?clef=valeur"
                },
                {
                        "http://localhost/service/api", "-v3/find", "nom-utilisateur=Camille&prenom-utilisateur=Saint Saëns",
                        "http://localhost/service/api-v3/find?nom-utilisateur=Camille&prenom-utilisateur=Saint+Sa%C3%ABns"
                },
                {
                        "http://example.com/", "", "bricolage&lés=8&couleur=bleu&catégorie=papiers peints",
                        "http://example.com/?bricolage&l%C3%A9s=8&couleur=bleu&cat%C3%A9gorie=papiers+peints"
                },
                {
                        "https://www.my-site.co.uk", null, "filtre=nom=Lucas+classe=2B",
                        "https://www.my-site.co.uk?filtre=nom%3DLucas%2Bclasse%3D2B"
                },
                {
                        "https://www.my-site.co.uk", "/_count", "",
                        "https://www.my-site.co.uk/_count"
                },
                {
                        "https://www.my-site.co.uk", "/_search", "q=identifiant:302158",
                        "https://www.my-site.co.uk/_search?q=identifiant%3A302158"
                }
        });
    }

    private final String baseUrl;
    private final String path;
    private final String queryParams;
    private final String exp;

    public WSToStateBaseGetFullAPIURL(
            final String baseUrl, final String path, final String queryParams, final String exp
    ) {
        this.baseUrl = baseUrl;
        this.path = path;
        this.queryParams = queryParams;
        this.exp = exp;
    }

    @Test
    public void getFullAPIURL() throws Exception {
    	Assert.assertEquals(exp, WSToStateBase.getFullAPIURL(baseUrl, path, queryParams));
    }
}
