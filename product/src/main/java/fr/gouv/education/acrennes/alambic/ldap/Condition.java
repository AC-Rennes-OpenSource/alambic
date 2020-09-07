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
package fr.gouv.education.acrennes.alambic.ldap;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.gouv.education.acrennes.alambic.utils.Variables;
import org.mvel2.MVEL;

public class Condition {
    private String condition;
    @SuppressWarnings("rawtypes")
	private Map vars = new HashMap();

    @SuppressWarnings("unchecked")
	public Condition(String condition,
		             Map<String, List<Map<String, List<String>>>> stateBaseList,
			         Variables variables) {
        this.condition = condition;
		if (condition!=null){
          vars.put("variables", variables.getHashMap());
          vars.put("statebases", stateBaseList);
		}
    }
	
	public boolean eval(){
		if (condition==null){
			return true;
		}
        Serializable expression = MVEL.compileExpression(condition);
		Boolean result = (Boolean) MVEL.executeExpression(expression, vars);
        return result;
	}
	

}
