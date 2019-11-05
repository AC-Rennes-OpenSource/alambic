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
package fr.gouv.education.acrennes.alambic.utils;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Base64;


public class EncodeUtils {

	public static String fonctionBase64Sha1(final String s) throws NoSuchAlgorithmException{
		MessageDigest md = MessageDigest.getInstance("SHA");
		byte[] b = md.digest(s.getBytes());
		
		return new String(Base64.encodeBase64(b));
	}

}
