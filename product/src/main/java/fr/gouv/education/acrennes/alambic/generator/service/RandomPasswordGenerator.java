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
package fr.gouv.education.acrennes.alambic.generator.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomLambdaEntity;

public class RandomPasswordGenerator extends AbstractRandomGenerator {

	private static final Log log = LogFactory.getLog(RandomPasswordGenerator.class);

	private static final String[] SYMBOLS_LETTER_m = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z" };
	private static final String[] SYMBOLS_LETTER_M = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z" };
	private static final String[] SYMBOLS_DIGIT = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" };
	private static final String[] SYMBOLS_SPECIAL = { "!", "*", "?", "#", "-", "@", "[", "]", "&", "(", ")", "%", "$" };

	private static final Map<String, String[]> DICTIONARY;
	static {
		DICTIONARY = new HashMap<String, String[]>();
		DICTIONARY.put("LETTER_MAJ", SYMBOLS_LETTER_M);
		DICTIONARY.put("LETTER_MIN", SYMBOLS_LETTER_m);
		DICTIONARY.put("DIGIT", SYMBOLS_DIGIT);
		DICTIONARY.put("SPECIAL", SYMBOLS_SPECIAL);
	}

	public RandomPasswordGenerator(final EntityManager em) throws AlambicException{
		super(em);
	}

	@Override
	public RandomEntity getEntity(final Map<String, Object> query, final String processId, final UNICITY_SCOPE scope) throws AlambicException {
		RandomLambdaEntity entity = null;

		// Get the dictionary according to the parameters
		List<String> dico = getQueryDictionary(query);

		// Build the random password
		int pwssdLength = (int) query.get("length");
		StringBuffer pwssd = new StringBuffer();
		for (int i = 0; i < pwssdLength; i++) {
			long randomSymbolIndex = getRandomNumber(0, dico.size() - 1);
			String randomSymbolName = dico.get((int) randomSymbolIndex);
			String[] randomSymbolCharacters = DICTIONARY.get(randomSymbolName);
			long randomElementIndex = getRandomNumber(0, randomSymbolCharacters.length - 1);
			pwssd.append(randomSymbolCharacters[(int) randomElementIndex]);
		}

		// Build entity object
		entity = new RandomLambdaEntity("{\"password\":\"" + pwssd + "\"}");

		// persist the entity built so that it can be found in "reuse" context
		em.persist(entity);

		return entity;
	}

	@Override
	public String getType() {
		return RandomGeneratorService.GENERATOR_TYPE.PASSWORD.toString();
	}

	@Override
	public long getCapacity(final Map<String, Object> query) throws AlambicException {
		long capacity = 0;

		// Get the dictionary according to the parameters
		List<String> dico = getQueryDictionary(query);

		// Get the longest symbol dictionary
		int len = 0;
		for (int i = 0; i < dico.size(); i++) {
			String[] symbolCharacters = DICTIONARY.get(dico.get(i));
			len = (len < symbolCharacters.length ? symbolCharacters.length : len);
		}

		/*
		 * Capacity is the possible number of symbols combinations
		 * ("optimistic" case is taken into consideration: the same longest symbol list is systematically used)
		 */
		int pwssdLength = (int) query.get("length");
		capacity = (long) Math.pow(len, pwssdLength);
		return capacity;
	}

	@Override
	public String getCapacityFilter(final Map<String, Object> query) {
		String filter = "{length=%s,symbols=[%s]}";

		// Get the dictionary according to the parameters
		List<String> dico = getQueryDictionary(query);
		Collections.sort(dico, new Comparator<String>() {

			@Override
			public int compare(final String o1, final String o2) {
				return o1.toLowerCase().compareTo(o2.toLowerCase());
			}
		});

		return String.format(filter, query.get("length"), StringUtils.join(dico, ","));
	}

	private List<String> getQueryDictionary(final Map<String, Object> query) {
		List<String> keys = new ArrayList<>();

		String symbols = (String) query.get("symbols");
		if (StringUtils.isNotBlank(symbols)) {
			for (String symbol : symbols.split(",")) {
				if (DICTIONARY.containsKey(symbol.toUpperCase())) {
					keys.add(symbol.toUpperCase());
				} else {
					log.error("The dictionary doesn't support the symbol '" + symbol + "', only '" + StringUtils.join(DICTIONARY.keySet(), ",") + "'");
				}
			}
		} else {
			keys.addAll(DICTIONARY.keySet());
		}

		return keys;
	}

}
