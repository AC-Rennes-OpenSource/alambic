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
package fr.gouv.education.acrennes.alambic.persistence;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EntityManagerHelper {

	 private static final Log log = LogFactory.getLog(EntityManagerHelper.class);

	private final EntityManagerFactory emFactory;
	private static EntityManagerHelper instance;

	private EntityManagerHelper(final String persistenceUnit, final Map<String, String> properties) {
		emFactory = Persistence.createEntityManagerFactory(persistenceUnit, properties);
	}

	public static void getInstance(final String persistenceUnit, final Map<String, String> properties) {
		if (null == instance) {
			instance = new EntityManagerHelper(persistenceUnit, properties);
		}
	}

	private static EntityManagerHelper getInstance() throws AlambicException {
		if (null == instance) {
			throw new AlambicException("Not initialized persistence unit");
		}
		return instance;
	}

	private EntityManager createEntityManager() {
		return emFactory.createEntityManager();
	}
	
	private void closeFactory() {
		if (null != emFactory && emFactory.isOpen()) {
			emFactory.close();
			instance = null;
		}
	}

	public static EntityManager getEntityManager() throws AlambicException {
		return getInstance().createEntityManager();
	}

	public static void close() {
		try {
			getInstance().closeFactory();
		} catch (AlambicException e) {
			log.error("Failed to close the persistence unit, error : " + e.getMessage());
		}
	}

}