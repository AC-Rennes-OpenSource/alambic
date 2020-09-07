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
package fr.gouv.education.acrennes.alambic.utils;

import java.util.Vector;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchResult;


public class UniqueName {
	private String name = "";
	private Vector listName = new Vector();

	public UniqueName(String name){
		this.name = name;
	}

	public void loadListRdnFromNamingEnumeration(NamingEnumeration nEnum) throws NamingException{
        while(nEnum.hasMore()) {
        	String currentName = new String(((SearchResult)nEnum.next()).getName());
            listName.addElement(currentName.toLowerCase());
        }
	}

	public String getUniqueName(){
        boolean testIfExist=true;
        String newName = new String(name);
        //Calcul du nouveau Nom sortie de boucle � 200
        for (int J = 0;J<200 && testIfExist;J++) {
            if (J>0) newName = name+J;
            testIfExist = listName.contains(newName.toLowerCase());
        }
    return newName;
    }

	public Vector getListName() {
		return listName;
	}

	public void setListName(Vector listName) {
		this.listName = listName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}


}
