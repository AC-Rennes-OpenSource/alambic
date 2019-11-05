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
package fr.gouv.education.acrennes.alambic.nuxeo;

import org.jdom2.Element;

/*
 * Structure XML à décoder dans le
 * 
 *     <ace granted="true" permission="Everything" principal="Administrator"/>
 *     
 */
public class ACE {
	private static final String GRANTED = "granted";
	private static final String PERMISSION = "permission";
	private static final String PRINCIPAL = "principal";
	private static final String TRUE = "true";
	private Element aceElement; 

	public ACE(Element aceElement) {
		this.aceElement = aceElement;
	}
	
	public boolean getGranted(){
		boolean granted = true;
		if(aceElement.getAttribute(GRANTED)!=null){
			granted = aceElement.getAttributeValue(GRANTED).equalsIgnoreCase(TRUE);
		}
		return granted;
	}
	
	public String getPermission(){
		return aceElement.getAttributeValue(PERMISSION);
	}
	
	public String getPrincipal(){
		return aceElement.getAttributeValue(PRINCIPAL);
	}

}
