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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.Query;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

import fr.gouv.education.acrennes.alambic.Constants;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.freemarker.FMFunctions;
import fr.gouv.education.acrennes.alambic.freemarker.NormalizationPolicy;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService;
import fr.gouv.education.acrennes.alambic.persistence.EntityManagerHelper;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomBlurIDEntity;

public class BlurIdToStateBase implements IToStateBase {

	private static final Log log = LogFactory.getLog(BlurIdToStateBase.class);
	private static final String HASH_ALGORITHM = "SHA-512";
	private static final int RELEVANT_NAME_STRING_LENGTH = 4;
	private static final List<SIGNATURE_STRATEGIES> DEFAULT_STRATEGIES = Arrays.asList( // for Backward compatibility purpose
			SIGNATURE_STRATEGIES.EDUCONNECT_LIKE,
			SIGNATURE_STRATEGIES.CIVILITY_FIRSTNAME_LASTNAME_PHONES,
			SIGNATURE_STRATEGIES.CIVILITY_FIRSTNAME_LASTNAME_EMAILS,
			SIGNATURE_STRATEGIES.CIVILITY_FIRSTNAME_LASTNAME_RELATIONSHIPS,
			SIGNATURE_STRATEGIES.IDENTITY);
	private static enum BLUR_MODE {
		HASHED_ID,
		SIGNATURE,
		NONE
	}
	private static enum SIGNATURE_STRATEGIES {
		IDENTITY,
		EDUCONNECT_LIKE,
		CIVILITY_FIRSTNAME_LASTNAME,
		CIVILITY_FIRSTNAME_LASTNAME_PHONES,
		CIVILITY_FIRSTNAME_LASTNAME_EMAILS,
		CIVILITY_FIRSTNAME_LASTNAME_RELATIONSHIPS
	}

	private List<Map<String, List<String>>> stateBase = new ArrayList<>();
	private EntityManager em;
	private FMFunctions fmfct;
	private MessageDigest md;
	private String defaultSaltStr;
	private byte[] salt;

	public BlurIdToStateBase(final String defaultSalt) throws AlambicException {
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
			blurIdMap.put("blurId", Collections.singletonList(getHashedId(query)));
		} else if (BLUR_MODE.SIGNATURE.equals(blurMode)) {
			List<RandomBlurIDEntity> resultSet = Collections.emptyList();
			blurIdMap.put("blurId", new ArrayList<>());

			// Build the list of signatures dealing with this query / entity
			Signatures signatures = buildSignatures(query);

			/**
			 * Synchronize the code block to avoid race condition ONLY when multiple threads request the blur identifier generator with a
			 * similar request. Otherwise, it is not worth locking.
			 * The group {civility, first name, last name} is used to detect the request similarity.
			 * This ensures that good performance (the synchronization mechanism pet peeve) are obtained even when a large number of thread are used.
			 */
			WriteLock lock = RandomGeneratorService.getLock(signatures.getRoot()).writeLock();
			lock.lock();

			try {
				// Look for any previous iteration
				if (!signatures.isEmpty()) {
					String predicat = String.format("'%s'", String.join("','", signatures.getSignatures()));
					String sqlQuery = "SELECT rbie FROM RandomBlurIDEntity rbie WHERE rbie.signature in (" + predicat + ")";
					Query emQuery = em.createQuery(sqlQuery);
					resultSet = emQuery.getResultList();
				}

				List<String> signaturesToPersist = signatures.getSignatures();

				if (resultSet.isEmpty()) {
					// First iteration ever to obtain a blur identifier for this query
					String newBlurId = UUID.randomUUID().toString();
					blurIdMap.get("blurId").add(newBlurId);
				} else {
					// Retrieve back the former blur identifier obtained
					Set<String> blurIdSet = resultSet.stream().map(RandomBlurIDEntity::getBlurid).collect(Collectors.toSet());
					if (blurIdSet.size() > 1) {
						log.warn("Several blurIds for signature set : " + String.join("", blurIdSet));
					}
					String formerBlurId = resultSet.get(0).getBlurid();
					blurIdMap.get("blurId").add(formerBlurId);
					log.debug("retrieved back the former blur identifier '" + formerBlurId + "' for the request with attributes : " + jsonquery);
					for (String signature:	resultSet.stream().map(RandomBlurIDEntity::getSignature).collect(
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
	public Iterator<List<Map<String, List<String>>>> getPageIterator(final String query, final String scope, final int pageSize, final String sortBy, final String orderBy)
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
	 * Build the signatures of one person's identity entity.
	 * As default, the implemented algorithm is compliant with the one used by the project EduConnect in order to gather 
	 * together AAF identities dealing with the same physical person.
	 * 
	 * EduConnect's algorithm description :
	 * A signature is the concatenation of the following items :
	 * - the civility
	 * - the first four characters of the first name
	 * - the first four characters of the last name
	 * - one of the phone numbers or one of the emails (one signature per phone number and email)
	 * 
	 * Since only the first four character of the first and last name are taken into account, no signature is built if no phone number and no email is known.
	 * This rule avoids to gather identities that are different indeed (two different physical persons).
	 * 
	 * Nevertheless, it is possible to produce a signature based on other strategies (it is possible to sum all of them)
	 * - on civility, first and last name by setting the query key "strategy": "CIVILITY_FIRSTNAME_LASTNAME"
	 * - on civility, first and last name plus phones by setting the query key "strategy": "CIVILITY_FIRSTNAME_LASTNAME_PHONES"
	 * - on civility, first and last name plus emails by setting the query key "strategy": "CIVILITY_FIRSTNAME_LASTNAME_EMAILS"
	 * - on civility, first and last name plus relationships (pupil - responsible) by setting the query key "strategy": "CIVILITY_FIRSTNAME_LASTNAME_RELATIONSHIPS"
	 * - on identifier only by setting the query key "strategy": "IDENTITY"
	 * 
	 * @param query contains the meaningful attributes of the person's identity entity
	 * @return the list of signatures (hashed) associated to this entity
	 */
	private Signatures buildSignatures(final JSONObject query) {
		List<String> plainSignatures = new ArrayList<>();
		List<String> signatureList = new ArrayList<>();

		String identifier = query.getString("id");
		String firstName = query.getString("firstName");
		String lastName = query.getString("lastName");
		String civility = (query.has("civility") && StringUtils.isNotBlank(query.getString("civility"))) ? query.getString("civility") : "undefined";
		List<Object> phones = (query.has("phones")) ? query.getJSONArray("phones").toList() : Collections.emptyList();
		List<Object> emails = (query.has("emails")) ? query.getJSONArray("emails").toList() : Collections.emptyList();
		List<Object> relationships = (query.has("relationships")) ? query.getJSONArray("relationships").toList() : Collections.emptyList();
		List<String> strategies = (query.has("strategies")) 
				? query.getJSONArray("strategies").toList().stream().map(Object::toString).collect(Collectors.toList()) 
				: DEFAULT_STRATEGIES.stream().map(SIGNATURE_STRATEGIES::toString).collect(Collectors.toList());

		// normalization to enhance the correlation rate
		String nFirstName = fmfct.normalize(firstName, NormalizationPolicy.WORD_ONLY, true).toLowerCase();
		String nLastName = fmfct.normalize(lastName, NormalizationPolicy.WORD_ONLY, true).toLowerCase();
		String nCivility = fmfct.normalize(civility, NormalizationPolicy.CIVILITE, true).toLowerCase();

		// apply "EduConnect like" truncate upon first name and last name attributes it the corresponding strategy applies
		String nRootFirstName = nFirstName;
		String nRootLastName = nLastName;
		if ( !strategies.isEmpty() && strategies.stream().anyMatch(item -> (item).equalsIgnoreCase(SIGNATURE_STRATEGIES.EDUCONNECT_LIKE.toString())) ) {
			nRootFirstName = getRelevantSubString(nFirstName);
			nRootLastName = getRelevantSubString(nLastName);
		}
		
		// build plain text signatures based on the known phones (if the strategy applies)
		if ( !strategies.isEmpty() && strategies.stream().anyMatch(item -> (item).equalsIgnoreCase(SIGNATURE_STRATEGIES.CIVILITY_FIRSTNAME_LASTNAME_PHONES.toString()))
			&& null != phones && !phones.isEmpty() ) {
			for (Object phone: phones) {
				String nPhone = fmfct.normalize(((String) phone).toLowerCase(), NormalizationPolicy.WORD_ONLY);
				plainSignatures.add(String.format("%s%s%s%s", nCivility, nRootFirstName, nRootLastName, nPhone));
			}
		}
		
		// build plain text signatures based on the known emails (if the strategy applies)
		if ( !strategies.isEmpty() && strategies.stream().anyMatch(item -> (item).equalsIgnoreCase(SIGNATURE_STRATEGIES.CIVILITY_FIRSTNAME_LASTNAME_EMAILS.toString()))
			&& null != emails && !emails.isEmpty() ) {
			for (Object email: emails) {
				String nEmail = ((String) email).toLowerCase().trim();
				plainSignatures.add(String.format("%s%s%s%s", nCivility, nRootFirstName, nRootLastName, nEmail));
			}
		}

		// build plain text signatures based on the known relationships (pupil <-> legal referent. if the strategy applies)
		if ( !strategies.isEmpty() && strategies.stream().anyMatch(item -> (item).equalsIgnoreCase(SIGNATURE_STRATEGIES.CIVILITY_FIRSTNAME_LASTNAME_RELATIONSHIPS.toString()))
			&& null != relationships && !relationships.isEmpty() ) {
			for (Object relationship: relationships) {
				String nRelationship = ((String) relationship).toLowerCase().trim();
				plainSignatures.add(String.format("%s%s%s%s", nCivility, nRootFirstName, nRootLastName, nRelationship));
			}
		}
		
		// catch the case where no "EduConnect like" reconciliation is possible since no secondary identification attribute (phone number, email, relationship) is available
		// => use the AAF identifier as default.
		if ( !strategies.isEmpty() && strategies.stream().anyMatch(item -> (item).equalsIgnoreCase(SIGNATURE_STRATEGIES.IDENTITY.toString()))
			&& plainSignatures.isEmpty() ) {
			plainSignatures.add(identifier);
		}
		
		if ( !strategies.isEmpty() && strategies.stream().anyMatch(item -> (item).equalsIgnoreCase(SIGNATURE_STRATEGIES.CIVILITY_FIRSTNAME_LASTNAME.toString())) ) {
			plainSignatures.add(String.format("%s%s%s", nCivility, nFirstName, nLastName));
		}
	
		// build hashed signatures
		for (String plainSignature: plainSignatures) {
			this.md.update(getSalt(getSeed(query)));
			String signature = Base64.encodeBase64String(md.digest(plainSignature.getBytes(StandardCharsets.UTF_8)));
			if (!signatureList.contains(signature)) {
				signatureList.add(signature);
			}
			this.md.reset();
		}

		return new Signatures(String.format("%s-%s-%s", nCivility, nFirstName, nLastName), signatureList);
	}

	private String getRelevantSubString(String string) {
		return (string.length() > RELEVANT_NAME_STRING_LENGTH) ? string.substring(0, RELEVANT_NAME_STRING_LENGTH) : string;
	}

	private byte[] getSalt(String seed) {
		if (this.salt == null || 0 == this.salt.length) {
			if (StringUtils.isNotBlank(seed)) {
				StringBuffer sb = new StringBuffer(seed);
				int i = 0;
				while (sb.length() < Constants.DEFAULT_SALT_LENGTH) {
					sb.append(sb.substring(i, i+1));
					i = ++i % (sb.length());
				}
				this.salt = sb.substring(0, Constants.DEFAULT_SALT_LENGTH).getBytes();
			} else {
				this.salt = this.defaultSaltStr.getBytes();
			}
		}
		return this.salt;
	}

	private class Signatures {
		private final String root;
		private final List<String> signatures;

		public Signatures(final String root, final List<String> signatures) {
			this.root = root;
			this.signatures = signatures;
		}

		public String getRoot() {
			return root;
		}

		public List<String> getSignatures() {
			return signatures;
		}

		public boolean isEmpty() {
			return this.signatures.isEmpty();
		}

		@Override
		public boolean equals(Object obj) {
			return this.root.equals(((Signatures) obj).getRoot());
		}

		@Override
		public String toString() {
			return String.format("{\"root\":\"%s\",\"signatures\":['%s']}", this.root, String.join("','", this.signatures));
		}

	}

	/**
	 * Get the salt seed from the query object.
	 * The seed deals with the "key" JSON key from the query object.
	 * /!\ For backward compatibility reason, the "processId" key is used instead when the "key" JSON key is missing.
	 */
	private String getSeed(final JSONObject query) {
		String seed = null;
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

}