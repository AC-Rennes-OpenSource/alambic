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
package fr.gouv.education.acrennes.alambic.jobs.load.gar.builder;

import java.text.DateFormat;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class GARHelper {

	private static final Log log = LogFactory.getLog(GARHelper.class);
	private static GARHelper instance;
	private DateFormat dateFormatter;
	private Map<String, String> cacheSourceSI;
	
	// Singleton
	private GARHelper() {
		this.dateFormatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
		this.cacheSourceSI = new HashMap<String, String>();
	}
	
	public static GARHelper getInstance() {
		if (null == instance) {
			instance = new GARHelper();
		}
		return instance;
	}
	
	/**
	 * Convertir le code "personalTitle" en valeur compatible avec le SDET.
	 * 
	 */
	public String getSDETCompliantTitleValue(final String personalTitle) {
		String compliantPersonalTitle = personalTitle;

		if (!"M.".equalsIgnoreCase(personalTitle)) {
			compliantPersonalTitle = "Mme";
		}

		return compliantPersonalTitle;
	}

	/**
	 * Convertir le code "contrat" en valeur compatible avec le SDET.
	 * "PUBLIC" devient "PU"
	 * "PRIVE" devient "PR"
	 * 
	 */
	public String getSDETCompliantContractValue(final String contrat) {
		return contrat.substring(0, 2);
	}

	/**
	 * Convertir le code représentant le profil d'accédant tel qu'il apparaît dans les exports AAF en un code respectant le SDET.
	 * 
	 * Sources documentaires :
	 * - SDET_annexe-operationnelle_6.0_660225.pdf - chapitre 4.7 Profils de l'accédant -
	 * (http://cache.media.eduscol.education.fr/file/sdet/22/5/SDET_annexe-operationnelle_6.0_660225.pdf)
	 * - SDET-Interoperabilite-v4.0_226607.pdf - chapitre 5.6 Profils de l'accédant -
	 * (http://cache.media.eduscol.education.fr/file/sdet/60/7/SDET-Interoperabilite-v4.0_226607.pdf)
	 * 
	 * @param function the job function
	 * @param title the title
	 */
	public String getSDETCompliantProfileValue(final String title, final String function) {
		String SDETvalue = null;

		String criteria = (StringUtils.isNotBlank(function)) ? function : title;
		if (StringUtils.isNotBlank(criteria)) {
			try {
				final TITLE_FUNCTION_MATCHING matchedValue = TITLE_FUNCTION_MATCHING.valueOf(criteria.toUpperCase());
				SDETvalue = matchedValue.getSDETValue().toString();
			} catch (IllegalArgumentException e) {
				if (StringUtils.isNotBlank(function)) {
					SDETvalue = getSDETCompliantProfileValue(title, null);
				} else {
					log.warn("Not possible to obtain the national profile, unknown title '" + title + "'");
				}
			}
		}

		return SDETvalue;
	}

	public String extractCodeGroup(final String code, final int index) {
		String group = null;

		final String[] groups = code.split("\\$");
		if ((null != groups) && (groups.length > index)) {
			group = groups[index];
		}

		return group;
	}

	public String getOutputFileName(final String fileTemplate, final int page, final int increment) {
		final String now = this.dateFormatter.format(new Date());
		return String.format(fileTemplate, now, page, increment);
	}

	public String getPersonEntityBlurId(Map<String, List<String>> entity) {
		String blurId = entity.toString();
		
		List<String> attribute = entity.get("ENTPersonUid");
		if (null != attribute && StringUtils.isNotBlank(attribute.get(0))) {
			blurId = String.format("(ENTPersonUid=%s)", attribute.get(0));
		} else {
			attribute = entity.get("ENTPersonJointure");
			if (null != attribute && StringUtils.isNotBlank(attribute.get(0))) {
				blurId = String.format("(ENTPersonJointure=%s)", attribute.get(0));
			}
		}
		
		return blurId;
	}

	public String getStructEntityBlurId(Map<String, List<String>> entity) {
		String blurId = entity.toString();
		
		List<String> attribute = entity.get("ENTStructureUAI");
		if (null != attribute && StringUtils.isNotBlank(attribute.get(0))) {
			blurId = String.format("(ENTStructureUAI=%s)", attribute.get(0));
		}
		
		return blurId;
	}

	public String getIndexationAlias(String sourceSI, INDEXATION_OBJECT_TYPE objectType) {
		// Build the cache entry key
		String key = Normalizer.normalize(sourceSI.concat(objectType.toString()), Form.NFD).replaceAll("[^\\p{ASCII}]", "")
				.trim()
				.toLowerCase();
		
		if (!this.cacheSourceSI.containsKey(key)) {
			if (sourceSI.matches("(?i:.*AGRI.*)")) {
				if (objectType.equals(INDEXATION_OBJECT_TYPE.MEF)) {
					this.cacheSourceSI.put(key, "agri_alias_mefeducnat");
				} else if (objectType.equals(INDEXATION_OBJECT_TYPE.Matiere)) {
					this.cacheSourceSI.put(key, "agri_alias_matiereeducnat");
				}
			} else {
				this.cacheSourceSI.put(key, objectType.getDefaultAlias());
			}
		}
		return this.cacheSourceSI.get(key);
	}

	public enum TITLE_FUNCTION_MATCHING {

		ADA(NATIONAL_PROFILE_IDENTIFIER.National_ETA),
		ADF(NATIONAL_PROFILE_IDENTIFIER.National_ETA),
		ADM(NATIONAL_PROFILE_IDENTIFIER.National_ETA),
		AED(NATIONAL_PROFILE_IDENTIFIER.National_EVS),
		AES(NATIONAL_PROFILE_IDENTIFIER.National_EVS),
		ALB(NATIONAL_PROFILE_IDENTIFIER.National_ETA),
		ASE(NATIONAL_PROFILE_IDENTIFIER.National_ETA),
		ASH(NATIONAL_PROFILE_IDENTIFIER.National_ETA),
		BED(NATIONAL_PROFILE_IDENTIFIER.National_ENS),
		CFC(NATIONAL_PROFILE_IDENTIFIER.National_ETA),
		CPD(NATIONAL_PROFILE_IDENTIFIER.National_ETA),
		CTR(NATIONAL_PROFILE_IDENTIFIER.National_ETA),
		DEC(NATIONAL_PROFILE_IDENTIFIER.National_DIR),
		DECPR(NATIONAL_PROFILE_IDENTIFIER.National_DIR),
		DIR(NATIONAL_PROFILE_IDENTIFIER.National_DIR),
		DOC(NATIONAL_PROFILE_IDENTIFIER.National_DOC),
		DSES(NATIONAL_PROFILE_IDENTIFIER.National_DIR),
		EDU(NATIONAL_PROFILE_IDENTIFIER.National_EVS),
		ELE(NATIONAL_PROFILE_IDENTIFIER.National_ELV),
		ENS(NATIONAL_PROFILE_IDENTIFIER.National_ENS),
		FCA(NATIONAL_PROFILE_IDENTIFIER.National_ENS),
		FCP(NATIONAL_PROFILE_IDENTIFIER.National_ENS),
		FIJ(NATIONAL_PROFILE_IDENTIFIER.National_ENS),
		INJ(NATIONAL_PROFILE_IDENTIFIER.National_ACA),
		INS(NATIONAL_PROFILE_IDENTIFIER.National_ACA),
		IS(NATIONAL_PROFILE_IDENTIFIER.National_ENS),
		ISES(NATIONAL_PROFILE_IDENTIFIER.National_ENS),
		LAB(NATIONAL_PROFILE_IDENTIFIER.National_ETA),
		MDS(NATIONAL_PROFILE_IDENTIFIER.National_ETA),
		ORI(NATIONAL_PROFILE_IDENTIFIER.National_ETA),
		OUV(NATIONAL_PROFILE_IDENTIFIER.National_ETA),
		PPA(NATIONAL_PROFILE_IDENTIFIER.National_ENS),
		RH1D(NATIONAL_PROFILE_IDENTIFIER.National_ENS),
		RPL(NATIONAL_PROFILE_IDENTIFIER.National_ENS),
		SUR(NATIONAL_PROFILE_IDENTIFIER.National_EVS),
		UPI(NATIONAL_PROFILE_IDENTIFIER.National_ENS);

		private final NATIONAL_PROFILE_IDENTIFIER profileId;

		private TITLE_FUNCTION_MATCHING(final NATIONAL_PROFILE_IDENTIFIER value) {
			this.profileId = value;
		}

		public NATIONAL_PROFILE_IDENTIFIER getSDETValue() {
			return this.profileId;
		}
	}

	public enum NATIONAL_PROFILE_IDENTIFIER {

		National_ELV,
		National_TUT,
		National_ENS,
		National_DIR,
		National_EVS,
		National_ETA,
		National_ACA,
		National_DOC,
		National_COL;
	}

	public enum INDEXATION_OBJECT_TYPE {

		MEF("aaf_alias_mefeducnat"),
		Matiere("aaf_alias_mateducnat");
		
		private final String default_alias;

		private INDEXATION_OBJECT_TYPE(final String value) {
			this.default_alias = value;
		}

		public String getDefaultAlias() {
			return this.default_alias;
		}
	}

}
