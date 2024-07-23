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
package fr.gouv.education.acrennes.alambic.jobs.extract.clients;

import fr.gouv.education.acrennes.alambic.Constants;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.freemarker.FMFunctions;
import fr.gouv.education.acrennes.alambic.freemarker.NormalizationPolicy;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService;
import fr.gouv.education.acrennes.alambic.persistence.EntityManagerHelper;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomBlurIDEntity;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.Query;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.stream.Collectors;

public class GecirBlurIdToStateBase implements IToStateBase {

    private static final Log log = LogFactory.getLog(GecirBlurIdToStateBase.class);
    private static final String HASH_ALGORITHM = "SHA-512";
    private static final String BLUR_ID = "blurId";

    private enum BLUR_MODE {
        HASHED_ID,
        SIGNATURE,
        NONE
    }

    private enum SIGNATURE_MODE {
        PEOPLE,
        COMPANY
    }

    private List<Map<String, List<String>>> stateBase = new ArrayList<>();
    private final EntityManager em;
    private final FMFunctions fmfct;
    private final MessageDigest md;
    private final String defaultSaltStr;
    private byte[] salt;

    public GecirBlurIdToStateBase(final String defaultSalt) throws AlambicException {
        this.defaultSaltStr = defaultSalt;
        this.fmfct = new FMFunctions();
        this.em = EntityManagerHelper.getEntityManager();
        this.em.setFlushMode(FlushModeType.AUTO);
        try {
            this.md = MessageDigest.getInstance(HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new AlambicException(e);
        }
    }

    @Override
    public void executeQuery(final String query) throws AlambicException {
        executeQuery(query, null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void executeQuery(final String jsonquery, final String scope) throws AlambicException {
        this.stateBase = new ArrayList<>();
        JSONObject query = new JSONObject(jsonquery);

        Map<String, List<String>> blurIdMap = new HashMap<>();
        this.stateBase.add(blurIdMap);

        BLUR_MODE blurMode = (query.has("blur_mode")) ? BLUR_MODE.valueOf((String) query.get("blur_mode")) : BLUR_MODE.NONE;
        if (BLUR_MODE.HASHED_ID.equals(blurMode)) {
            blurIdMap.put(BLUR_ID, Collections.singletonList(getHashedId(query)));
        } else if (BLUR_MODE.SIGNATURE.equals(blurMode)) {
            List<RandomBlurIDEntity> resultSet = Collections.emptyList();
            blurIdMap.put(BLUR_ID, new ArrayList<>());

            // Build the list of signatures dealing with this query / entity
            Signatures signatures = buildSignatures(query);

            /**
             * Synchronize the code block to avoid race condition ONLY when multiple threads request the blur identifier generator with a
             * similar request. Otherwise, it is not worth locking.
             */
            WriteLock lock = RandomGeneratorService.getLock(signatures.getRoot()).writeLock();
            lock.lock();

            try {
                // Look for any previous iteration
                if (!signatures.isEmpty()) {
                    String predicat = String.format("'%s'", String.join("','", signatures.getItems()));
                    String sqlQuery = "SELECT rbie FROM RandomBlurIDEntity rbie WHERE rbie.signature in (" + predicat + ")";
                    Query emQuery = em.createQuery(sqlQuery);
                    resultSet = emQuery.getResultList();
                }

                List<String> signaturesToPersist = signatures.getItems();

                if (resultSet.isEmpty()) {
                    // First iteration ever to obtain a blur identifier for this query
                    String newBlurId = UUID.randomUUID().toString();
                    blurIdMap.get(BLUR_ID).add(newBlurId);
                } else {
                    // Retrieve back the former blur identifier obtained
                    Set<String> blurIdSet = resultSet.stream().map(RandomBlurIDEntity::getBlurid).collect(Collectors.toSet());
                    if (blurIdSet.size() > 1) {
                        log.warn("Several blurIds for signature set : " + String.join(",", blurIdSet));
                    }
                    String formerBlurId = resultSet.get(0).getBlurid();
                    blurIdMap.get(BLUR_ID).add(formerBlurId);
                    log.debug("retrieved back the former blur identifier '" + formerBlurId + "' for the request with attributes : " + jsonquery);
                    for (String signature : resultSet.stream().map(RandomBlurIDEntity::getSignature).collect(
                            Collectors.toSet())) {
                        signaturesToPersist.remove(signature);
                    }
                }
                if (!signaturesToPersist.isEmpty()) {
                    persist(signaturesToPersist, blurIdMap.get("blurId").get(0));
                }
            } finally {
                lock.unlock();
                RandomGeneratorService.releaseLock(signatures.getRoot());
            }
        } else {
            throw new AlambicException("Not supported or undefined blur mode (supported modes are 'HASHED_ID' & 'SIGNATURE')");
        }
    }

    @Override
    public List<Map<String, List<String>>> getStateBase() {
        return stateBase;
    }

    @Override
    public void close() {
        // Not used
    }

    @Override
    public int getCountResults() {
        return stateBase.size();
    }

    @Override
    public void clear() {
        stateBase.clear();
    }

    @Override
    public Iterator<List<Map<String, List<String>>>> getPageIterator(final String query, final String scope, final int pageSize,
                                                                     final String sortBy, final String orderBy)
            throws AlambicException {
        throw new AlambicException("Not implemented operation");
    }

    private String getHashedId(final JSONObject query) {
        this.md.update(getSalt(getSeed(query)));
        String hashedId = Base64.encodeBase64String(md.digest(query.getString("id").getBytes(StandardCharsets.UTF_8)));
        this.md.reset();
        return hashedId;
    }

    /**
     * Build the signatures of one person's or company's identity entity.
     *
     * Algorithm description :
     * A signature is the concatenation of the following items :
     * - the civility
     * - the first four characters of the first name
     * - the first four characters of the last name
     * - one of the phone numbers or one of the emails (one signature per phone number and email)
     *
     * Since only the first four character of the first and last name are taken into account, no signature is built if no phone number and no email
     * is known.
     * This rule avoids to gather identities that are different indeed (two different physical persons).
     * @param query contains the meaningful attributes of the person's identity entity
     * @return the list of signatures (hashed) associated to this entity
     */
    private Signatures buildSignatures(final JSONObject query) {
        SIGNATURE_MODE signatureMode = (query.has("signature_mode"))
                                       ? SIGNATURE_MODE.valueOf((String) query.get("signature_mode"))
                                       : SIGNATURE_MODE.PEOPLE;
        if (signatureMode.equals(SIGNATURE_MODE.COMPANY)) {
            return buildCompanySignatures(query);
        } else {
            return buildPeopleSignatures(query);
        }
    }

    private Signatures buildCompanySignatures(final JSONObject query) {
        String root = "undefined";
        List<String> plainSignatures = new ArrayList<>();
        List<String> signatureList = new ArrayList<>();
        String id = query.optString("id_entreprise");
        String designation = query.optString("designation");
        String siren = query.optString("siren");
        String tvaIntracom = query.optString("tva_intracommunautaire");

        if (!id.isEmpty()) {
            plainSignatures.add(id);
        }
        if (!designation.isEmpty()) {
            plainSignatures.add(fmfct.normalize(designation, NormalizationPolicy.WORD_ONLY, true).toUpperCase());
            root = designation;
        }
        if (!siren.isEmpty()) {
            plainSignatures.add(siren);
        }
        if (!tvaIntracom.isEmpty()) {
            plainSignatures.add(tvaIntracom);
        }

        for (String plainSignature : plainSignatures) {
            this.md.update(getSalt(getSeed(query)));
            String signature = Base64.encodeBase64String(md.digest(plainSignature.getBytes(StandardCharsets.UTF_8)));
            if (!signatureList.contains(signature)) {
                signatureList.add(signature);
            }
            this.md.reset();
        }
        return new Signatures(root, signatureList);
    }

    private Signatures buildPeopleSignatures(final JSONObject query) {
        String root = "undefined";
        List<String> plainSignatures = new ArrayList<>();
        List<String> signatureList = new ArrayList<>();
        String firstName = query.optString("first_name");
        String lastName = query.optString("last_name");
        String fullName = query.optString("full_name");
        String idDemandeur = query.optString("id_demandeur");

        String nFirstName = fmfct.normalize(firstName, NormalizationPolicy.WORD_ONLY, true).toLowerCase();
        String nLastName = fmfct.normalize(lastName, NormalizationPolicy.WORD_ONLY, true).toLowerCase();
        String nFullName = fmfct.normalize(fullName, NormalizationPolicy.WORD_ONLY, true).toLowerCase();

        if (!nFirstName.isEmpty() && !nLastName.isEmpty()) {
            plainSignatures.add(String.format("%s%s", nFirstName, nLastName));
            plainSignatures.add(String.format("%s%s", nLastName, nFirstName));
            root = String.format("%s%s", nFirstName, nLastName);
        }
        if (!nFullName.isEmpty()) {
            plainSignatures.add(nFullName);
            root = nFullName;
        }
        if (!idDemandeur.isEmpty()) {
            plainSignatures.add(String.format("Demandeur%s", idDemandeur));
        }

        for (String plainSignature : plainSignatures) {
            this.md.update(getSalt(getSeed(query)));
            String signature = Base64.encodeBase64String(md.digest(plainSignature.getBytes(StandardCharsets.UTF_8)));
            if (!signatureList.contains(signature)) {
                signatureList.add(signature);
            }
            this.md.reset();
        }
        return new Signatures(root, signatureList);
    }

    private byte[] getSalt(String seed) {
        if (this.salt == null || 0 == this.salt.length) {
            if (StringUtils.isNotBlank(seed)) {
                StringBuilder sb = new StringBuilder(seed);
                int i = 0;
                while (sb.length() < Constants.DEFAULT_SALT_LENGTH) {
                    sb.append(sb.substring(i, i + 1));
                    i = ++i % (sb.length());
                }
                this.salt = sb.substring(0, Constants.DEFAULT_SALT_LENGTH).getBytes();
            } else {
                this.salt = this.defaultSaltStr.getBytes();
            }
        }
        return this.salt;
    }

    /**
     * Get the salt seed from the query object.
     * The seed deals with the "key" JSON key from the query object.
     * /!\ For backward compatibility reason, the "processId" key is used instead when the "key" JSON key is missing.
     */
    private String getSeed(final JSONObject query) {
        String seed;
        String key = query.has("key") ? query.getString("key").trim() : null;
        String processId = query.has("processId") ? query.getString("processId").trim() : null;
        seed = StringUtils.isNotBlank(key) ? key : processId;
        return seed;
    }

    private void persist(List<String> signatures, String newBlurId) {
        EntityTransaction tx = this.em.getTransaction();
        tx.begin();
        for (String signature : signatures) {
            RandomBlurIDEntity rbie = new RandomBlurIDEntity(signature, newBlurId);
            this.em.persist(rbie);
        }
        tx.commit();
    }

    private static class Signatures {
        private final String root;
        private final List<String> items;

        public Signatures(final String root, final List<String> signatures) {
            this.root = root;
            this.items = signatures;
        }

        public String getRoot() {
            return root;
        }

        public List<String> getItems() {
            return items;
        }

        public boolean isEmpty() {
            return this.items.isEmpty();
        }

        @Override
        public boolean equals(Object obj) {
            return this.root.equals(((GecirBlurIdToStateBase.Signatures) obj).getRoot());
        }

        @Override
        public String toString() {
            return String.format("{\"root\":\"%s\",\"signatures\":['%s']}", this.root, String.join("','", this.items));
        }

    }

}