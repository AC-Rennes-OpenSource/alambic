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
package fr.gouv.education.acrennes.alambic.generator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.persistence.EntityManagerHelper;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RandomGeneratorService {

    private static final Log log = LogFactory.getLog(RandomGeneratorService.class);

    // Singleton pattern
    private RandomGeneratorService() {
    }

    private static RandomGeneratorService getInstance() {
        return RGSSingletonHolder.instance;
    }

    private static void addKeyValuePair(final String key, final Object value, final Map<String, List<String>> map) {
        if (value instanceof String) {
            map.put(key, List.of((String) value));
        } else if (value instanceof Integer) {
            map.put(key, List.of(Integer.toString((Integer) value)));
        } else if (value instanceof Long) {
            map.put(key, List.of(Long.toString((Long) value)));
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapValue = (Map<String, Object>) value;
            for (String innerKey : mapValue.keySet()) {
                addKeyValuePair(key.concat("_" + innerKey), mapValue.get(innerKey), map);
            }
        } else {
            log.error("Malformed entry structure!");
            // ignored
        }
    }

    public static RandomGenerator getRandomGenerator(final GENERATOR_TYPE type) throws AlambicException {
        return getInstance().getRandomGeneratorSingleInstance(type);
    }

    public static ReentrantReadWriteLock getLock(final String token) {
        return getInstance().getRandomGeneratorLock(token);
    }

    public static void releaseLock(final String token) {
        getInstance().releaseRandomGeneratorLock(token);
    }

    public static Map<String, Long> getCapacityCache() {
        return getInstance().getCapacityCacheSingleInstance();
    }

    public static Map<String, List<String>> toStateBaseEntry(final RandomEntity entry) {
        Map<String, List<String>> sbeMap = new HashMap<>();

        Map<String, Object> entityMap;
        try {
            entityMap = new ObjectMapper().readValue(entry.getJson(), new TypeReference<>() {
            });
            for (String key : entityMap.keySet()) {
                addKeyValuePair(key, entityMap.get(key), sbeMap);
            }
        } catch (IOException e) {
            log.error("Failed to convert the random entity object (" + entry + ") into state base entity, error: " + e.getMessage());
        }

        return sbeMap;
    }

    public static void close() {
        getInstance().closeInstance();
    }

    private RandomGenerator getRandomGeneratorSingleInstance(final GENERATOR_TYPE type) throws AlambicException {
        RandomGenerator generator = switch (type) {
            case USER -> new RandomUserGenerator(getEntityManager());
            case DATE -> new RandomDateGenerator(getEntityManager());
            case PASSWORD -> new RandomPasswordGenerator(getEntityManager());
            case UID -> new RandomUidGenerator(getEntityManager());
            case UUID -> new RandomUUidGenerator(getEntityManager());
            case INTEGER -> new RandomIntegerGenerator(getEntityManager());
            case UNIK -> new UnikGenerator(getEntityManager());
            case MAIL -> new RandomMailGenerator(getEntityManager());
            case UAI -> new RandomUAIGenerator(getEntityManager());
            case IMAGE -> new RandomImageGenerator(getEntityManager());
            case IDENTITY -> new RandomIdentityGenerator(getEntityManager());
            case ADDRESS -> new RandomAddressGenerator(getEntityManager());
            default -> throw new AlambicException("Not supported yet random generator type : '" + type + "'");
        };

        return generator;
    }

    synchronized private ReentrantReadWriteLock getRandomGeneratorLock(final String token) {
        if (RGSSingletonHolder.persistence_locks.containsKey(token)) {
            RGSSingletonHolder.persistence_locks.get(token).register(Thread.currentThread().getId());
        } else {
            LockRegister lr = new LockRegister();
            lr.register(Thread.currentThread().getId());
            RGSSingletonHolder.persistence_locks.put(token, lr);
        }
        return RGSSingletonHolder.persistence_locks.get(token).getLock();
    }

    synchronized private void releaseRandomGeneratorLock(final String token) {
        if (RGSSingletonHolder.persistence_locks.containsKey(token)) {
            LockRegister lr = RGSSingletonHolder.persistence_locks.get(token);
            lr.unregister(Thread.currentThread().getId());
            if (lr.isEmpty()) {
                RGSSingletonHolder.persistence_locks.remove(token);
            }
        } else {
            log.debug("Tried to release a lock for a not registered token '" + token + "'");
        }
    }

    private Map<String, Long> getCapacityCacheSingleInstance() {
        return RGSSingletonHolder.capacity_cache;
    }

    private EntityManager getEntityManager() throws AlambicException {
        EntityManager em = EntityManagerHelper.getEntityManager();
        em.setFlushMode(FlushModeType.AUTO);
        return em;
    }

    private void closeInstance() {
        RGSSingletonHolder.capacity_cache.clear();
        RGSSingletonHolder.persistence_locks.clear();
    }

    public enum GENERATOR_TYPE {
        USER,
        IDENTITY,
        PASSWORD,
        ADDRESS,
        DATE,
        UID,
        UUID,
        INTEGER,
        UNIK,
        MAIL,
        UAI,
        IMAGE
    }

    // This singleton will be lazy instantiated via the "Holder" pattern (that requires not code synchronisation)
    private static class RGSSingletonHolder {
        // unique instance not pre-initialized
        private final static RandomGeneratorService instance = new RandomGeneratorService();
        private final static ConcurrentMap<String, LockRegister> persistence_locks = new ConcurrentHashMap<>();
        private static final ConcurrentHashMap<String, Long> capacity_cache = new ConcurrentHashMap<>(20);
    }

    private static class LockRegister {
        private final List<Long> contention_list;
        private final ReentrantReadWriteLock lock;

        public LockRegister() {
            this.contention_list = new ArrayList<>();
            this.lock = new ReentrantReadWriteLock();
        }

        public void register(final Long item) {
            if (!this.contention_list.contains(item)) {
                this.contention_list.add(item);
            } else {
                log.debug("Tried to register a contention item that is already present");
            }
        }

        public void unregister(final Long item) {
            if (this.contention_list.contains(item)) {
                this.contention_list.remove(item);
            } else {
                log.debug("Tried to unregister a contention item that is not present");
            }
        }

        public ReentrantReadWriteLock getLock() {
            return this.lock;
        }

        public boolean isEmpty() {
            return this.contention_list.isEmpty();
        }

    }

}
