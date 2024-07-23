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
package fr.gouv.education.acrennes.alambic.jobs.load.gar.builder;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.extract.sources.Source;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GARHelper {

    private static final Log log = LogFactory.getLog(GARHelper.class);
    private static GARHelper instance;
    private final DateFormat dateFormatter;
    private final Map<String, String> cacheSourceSI;

    // Singleton
    private GARHelper() {
        this.dateFormatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
        this.cacheSourceSI = new HashMap<>();
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
     * - SDET_Annexe-operationnelle_v6.3_1158753.pdf - chapitre 4.7 Profils de l'accédant -
     * (https://cache.media.eduscol.education.fr/file/ENT/75/3/SDET_Annexe-operationnelle_v6.3_1158753.pdf)
     * - GAR_RTFS_Informations_Detaillees_ENT_GARv4.0.pdf
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

        final String[] groups = (StringUtils.isNotBlank(code)) ? code.split("\\$") : new String[0];
        if (groups.length > index) {
            group = groups[index].trim();
        }

        return group;
    }

    public String getOutputFileName(final String fileTemplate, final int page, final int increment) {
        final String now = this.dateFormatter.format(new Date());
        return String.format(fileTemplate, now, page, increment);
    }

    public String getPersonEntityBlurId(Map<String, List<String>> entity) {
        String blurId = entity.toString();

        List<String> attribute = entity.get("elenid");
        if (null != attribute && StringUtils.isNotBlank(attribute.get(0))) {
            blurId = String.format("(elenid=%s)", attribute.get(0));
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

    public boolean isCodeValid(final Source dataSource, final String sourceSI, final String territoryCode, final INDEXATION_OBJECT_TYPE objectType,
                               final String code)
            throws AlambicException {
        boolean isValid = false;

        try {
            // query AAF's index
            String query = String.format("{\"api\":\"/%s/_search\",\"parameters\":\"q=identifiant:%s\"}", this.getIndexationAlias(sourceSI,
                    territoryCode, objectType), code);
            List<Map<String, List<String>>> resultSet = dataSource.query(query);

            // perform controls
            if (CollectionUtils.isNotEmpty(resultSet)) {
                Map<String, List<String>> item = resultSet.get(0); // a single item is expected
                JSONObject jsonResultSet = new JSONObject(item.get("item").get(0));
                if (1 == jsonResultSet.getJSONObject("hits").getInt("total")) {
                    isValid = true;
                }
            }
        } catch (Exception e) {
            throw new AlambicException(e.getMessage());
        }

        return isValid;
    }

    public String getIndexationAlias(String sourceSI, String territoryCode, INDEXATION_OBJECT_TYPE objectType) {
        // Build the cache entry key
        String key = Normalizer.normalize(sourceSI.concat(objectType.toString()).concat(territoryCode), Form.NFD).replaceAll("[^\\p{ASCII}]", "")
                .trim()
                .toLowerCase();

        if (!this.cacheSourceSI.containsKey(key)) {
            if (sourceSI.matches("(?i:.*AGRI.*)")) {
                this.cacheSourceSI.put(key, objectType.getDefaultAlias().replace("aaf_", "agri_").concat("_" + territoryCode));
            } else {
                this.cacheSourceSI.put(key, objectType.getDefaultAlias().concat("_" + territoryCode));
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
        COLTER(NATIONAL_PROFILE_IDENTIFIER.National_COL),
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
        PSY(NATIONAL_PROFILE_IDENTIFIER.National_ETA),
        RH1D(NATIONAL_PROFILE_IDENTIFIER.National_ENS),
        RPL(NATIONAL_PROFILE_IDENTIFIER.National_ENS),
        SUR(NATIONAL_PROFILE_IDENTIFIER.National_EVS),
        TEC(NATIONAL_PROFILE_IDENTIFIER.National_ETA),
        UPI(NATIONAL_PROFILE_IDENTIFIER.National_ENS);

        private final NATIONAL_PROFILE_IDENTIFIER profileId;

        TITLE_FUNCTION_MATCHING(final NATIONAL_PROFILE_IDENTIFIER value) {
            this.profileId = value;
        }

        public NATIONAL_PROFILE_IDENTIFIER getSDETValue() {
            return this.profileId;
        }
    }

    public enum NATIONAL_PROFILE_IDENTIFIER {

        National_ELV(false),
        National_TUT(false),
        National_ENS(true),
        National_DIR(true),
        National_EVS(true),
        National_ETA(true),
        National_ACA(false),
        National_DOC(true),
        National_COL(true);

        private final boolean supported;

        NATIONAL_PROFILE_IDENTIFIER(final boolean supported) {
            this.supported = supported;
        }

        public boolean isSupported() {
            return this.supported;
        }
    }

    public enum INDEXATION_OBJECT_TYPE {

        Eleve("aaf_alias_eleve"),
        PersEducNat("aaf_alias_perseducnat"),
        EtabEducNat("aaf_alias_etabeducnat"),
        MEF("aaf_alias_mefeducnat"),
        Matiere("aaf_alias_mateducnat");

        private final String default_alias;

        INDEXATION_OBJECT_TYPE(final String value) {
            this.default_alias = value;
        }

        public String getDefaultAlias() {
            return this.default_alias;
        }
    }

}
