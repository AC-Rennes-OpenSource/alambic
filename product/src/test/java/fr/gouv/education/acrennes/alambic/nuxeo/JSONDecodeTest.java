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
package fr.gouv.education.acrennes.alambic.nuxeo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import junit.framework.TestCase;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.Assert;

import java.io.IOException;

public class JSONDecodeTest extends TestCase {

    private static final ObjectMapper mapper = new ObjectMapper();

    public void test1() throws IOException {
        JSONArray content = new JSONArray();
        JSONObject obj = new JSONObject();
        obj.element("uid", "mberhaut1");
        obj.element("niveau", "1");
        obj.element("ident", "marc berhaut");
        obj.element("email", "marc.berhaut@ac-rennes.fr");
        obj.element("owner", "true");
        content.add(obj);

        obj = new JSONObject();
        obj.element("uid", "oadam");
        obj.element("niveau", "2");
        obj.element("ident", "Olivier Adam");
        obj.element("email", "olivier.adam@ac-rennes.fr");
        obj.element("owner", "false");
        content.add(obj);

        System.out.println("CONTENT=" + content);

        ArrayNode jsonArray = (ArrayNode) mapper.readTree(content.toString());
        Assert.assertNotNull(jsonArray);
        System.out.println("JSON=" + jsonArray);

        for (int i = 0; i < jsonArray.size(); i++) {
            JsonNode node = jsonArray.get(i);
            System.out.println(" > node=" + node.toString());
        }
    }

}
