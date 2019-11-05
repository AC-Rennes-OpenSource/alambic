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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.persistence.EntityManagerHelper;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;

public class RandomGeneratorService {

	private static final Log log = LogFactory.getLog(RandomGeneratorService.class);

	private static RandomGeneratorService instance;

	public enum GENERATOR_TYPE {
		USER,
		PASSWORD,
		ADDRESS,
		DATE,
		UID,
		UUID,
		INTEGER,
		UNIK
	}

	private RandomGeneratorService() {
	}

	public static RandomGeneratorService getInstance() {
		if (null == instance) {
			instance = new RandomGeneratorService();
		}

		return instance;
	}

	public RandomGenerator getRandomGenerator(final GENERATOR_TYPE type) throws AlambicException {
		RandomGenerator generator = null;

		switch (type) {
		case USER:
			generator = new RandomUserGenerator(getEntityManager());
			break;
		case DATE:
			generator = new RandomDateGenerator(getEntityManager());
			break;
		case PASSWORD:
			generator = new RandomPasswordGenerator(getEntityManager());
			break;
		case UID:
			generator = new RandomUidGenerator(getEntityManager());
			break;
		case UUID:
			generator = new RandomUUidGenerator(getEntityManager());
			break;
		case INTEGER:
			generator = new RandomIntegerGenerator(getEntityManager());
			break;
		case UNIK:
            generator = new UnikGenerator(getEntityManager());
            break;
		default:
			log.error("Not supported yet random generator type: '" + type + "'");
		}

		return generator;
	}

	private EntityManager getEntityManager() throws AlambicException {
		EntityManager em = EntityManagerHelper.getEntityManager();
		em.setFlushMode(FlushModeType.AUTO);
		return em;
	}

	public Map<String, List<String>> toStateBaseEntry(final RandomEntity entry) {
		Map<String, List<String>> sbeMap = new HashMap<String, List<String>>();

		Map<String, Object> entityMap;
		try {
			entityMap = new ObjectMapper().readValue(entry.getJson(), new TypeReference<Map<String, Object>>() {});
			Iterator<String> keysItr = entityMap.keySet().iterator();
			while (keysItr.hasNext()) {
				String key = keysItr.next();
				addKeyValuePair(key, entityMap.get(key), sbeMap);
			}
		} catch (IOException e) {
			log.error("Failed to convert the random entity object (" + entry + ") into state base entity, error: " + e.getMessage());
		}

		return sbeMap;
	}

	private void addKeyValuePair(final String key, final Object value, final Map<String, List<String>> map) {// final Map<String, Object> randomEntryMap) {
		if (value instanceof String) {
			map.put(key, Arrays.asList((String) value));
		} else if (value instanceof Integer) {
			map.put(key, Arrays.asList(Integer.toString((Integer) value)));
		} else if (value instanceof Long) {
			map.put(key, Arrays.asList(Long.toString((Long) value)));
		} else if (value instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> mapValue = (Map<String, Object>) value;
			Iterator<String> keysItr = mapValue.keySet().iterator();
			while (keysItr.hasNext()) {
				String innerKey = keysItr.next();
				addKeyValuePair(key.concat("_" + innerKey), mapValue.get(innerKey), map);
			}
		} else {
			log.error("Malformed entry structure!");
			// ignored
		}
	}

	public void close() {
		instance = null;
	}

}
