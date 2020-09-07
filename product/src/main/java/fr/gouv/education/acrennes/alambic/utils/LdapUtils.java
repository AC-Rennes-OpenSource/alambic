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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

/*
 * Cette methode effectue une comparaison entre 2 attributs LDAP
 * retourne les valeurs suivantes
 * 3 : A égal B
 * 2 : B contient toutes les valeurs de A
 * 1 : A contient toutes les valeurs de B
 * 0 : A différent B
 */
public class LdapUtils {

	private static final Pattern REGEXP_PATTERN = Pattern.compile("\\(REGEXP\\)(.+)\\(/REGEXP\\)");

	public static int compareAttributes(final Attribute attrA, final Attribute attrB) throws NamingException, UnsupportedEncodingException {
		int res = 0;
		if (attributeContainsValues(attrA, attrB.getAll())) {
			res = 1;
		}
		if (attributeContainsValues(attrB, attrA.getAll())) {
			res += 2;
		}
		return res;
	}

	/*
	 * Construit la liste des valeurs en + de la liste de gauche (left)
	 */
	public static List<String> positiveDelta(final Attribute leftAttr, final Attribute rightAttr) throws NamingException, UnsupportedEncodingException {
		NamingEnumeration<?> leftValue = leftAttr.getAll();
		try {
			List<String> delta = new ArrayList<String>();
			boolean test = true;
			while (leftValue.hasMore()) {
				String stringLeftValue = Functions.getInstance().valueToString(leftValue.next());
				test = false;
				NamingEnumeration<?> rightValue = rightAttr.getAll();
				try {
					while (rightValue.hasMore() && !test) {
						String stringRightValue = Functions.getInstance().valueToString(rightValue.next());
						test = stringRightValue.equalsIgnoreCase(stringLeftValue);
					}
				} finally {
					rightValue.close();
				}
				if (!test) {
					delta.add(stringLeftValue);
				}
			}

			return delta;
		} finally {
			leftValue.close();
		}
	}

	public static boolean attributeContainsValues(final Attribute attr, final NamingEnumeration<?> listValues) throws NamingException, UnsupportedEncodingException {
		boolean test = true;
		while (listValues.hasMore() && test) {
			// test de l'existance de la valeur
			String currentValueFromTemporaryAttrLdap;
			currentValueFromTemporaryAttrLdap =
					Functions.getInstance().valueToString(listValues.next());
			NamingEnumeration<?> listValuesCurrentAttrLdap = attr.getAll();
			try {
				test = false;
				while (listValuesCurrentAttrLdap.hasMore() && !test) {
					String currentValueFromCurrentAttrLdap =
							Functions.getInstance().valueToString(listValuesCurrentAttrLdap.next());
					test = currentValueFromTemporaryAttrLdap.equalsIgnoreCase(currentValueFromCurrentAttrLdap);
				}
			} finally {
				listValuesCurrentAttrLdap.close();
			}
		}
		return test;
	}

	public static boolean attributeContainsValue(final Attribute attr, final String attendedValue) throws NamingException, UnsupportedEncodingException {
		NamingEnumeration<?> values = attr.getAll();
		try {
			boolean test = false;
			while (values.hasMore() && !test) {
				String existingValue =
						Functions.getInstance().valueToString(values.next());
				test = existingValue.equalsIgnoreCase(attendedValue);
			}
			return test;
		} finally {
			values.close();
		}
	}

	public static boolean attributeContainsValue(final Attribute attr, final Object value) throws NamingException, UnsupportedEncodingException {
		NamingEnumeration<?> values = attr.getAll();
		try {
			boolean test = false;
			String attendedValue =
					Functions.getInstance().valueToString(value);

			while (values.hasMore() && !test) {
				String existingValue =
						Functions.getInstance().valueToString(values.next());

				test = existingValue.equalsIgnoreCase(attendedValue);
			}

			return test;
		} finally {
			values.close();
		}
	}

	public static boolean attributeContainsValueAllowRegExp(final Attribute attr, final Object value) throws NamingException, UnsupportedEncodingException {
		NamingEnumeration<?> values = attr.getAll();
		try {
			boolean test = false;
			String attendedValue = Functions.getInstance().valueToString(value);

			while (values.hasMore() && !test) {
				String existingValue = Functions.getInstance().valueToString(values.next());
				final Matcher hasRegExp = REGEXP_PATTERN.matcher(existingValue);
				if (hasRegExp.matches()) {
					final String regExp = hasRegExp.group(1);
					test = Pattern.matches(regExp, attendedValue);
				} else {
					test = existingValue.equalsIgnoreCase(attendedValue);
				}
			}

			return test;
		} finally {
			values.close();
		}
	}

	public static SearchResult getEntryFromDn(final DirContext ctx, final SearchControls contraintes, final String dn) throws NamingException {
		String rdn = dn.substring(0, dn.indexOf(","));
		String context = dn.substring(dn.indexOf(",") + 1);
		String subContext = context.substring(0, context.indexOf(ctx.getNameInNamespace()) - 1);
		SearchResult entry = ctx.search(subContext, "(" + rdn + ")", contraintes).next();
		return entry;
	}

}
