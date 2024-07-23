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
package fr.gouv.education.acrennes.alambic.freemarker;

/**
 * --------------------------------------------------------------------------------------------------------
 * NORMALIZATION POLICIES (based-on the tool box)
 * --------------------------------------------------------------------------------------------------------
 */
public enum NormalizationPolicy {

    UID(new String[] { NormalizationToolbox.NORMALIZE_REGEX_SHRINK_QUOTES_WITH_WS, NormalizationToolbox.NORMALIZE_REGEX_TRIM_SPECIAL_CHARACTERS,
            NormalizationToolbox.NORMALIZE_REGEX_REPLACE_WHITESPACES_AND_QUOTES_BY_HYPHEN, NormalizationToolbox.NORMALIZE_REGEX_SHRINK_HYPHENS }),
    UNIK(new String[] { NormalizationToolbox.NORMALIZE_REGEX_REMOVE_SPECIAL_CHARACTERS }),
    EMAIL(new String[] { NormalizationToolbox.NORMALIZE_REGEX_REMOVE_QUOTES,
            NormalizationToolbox.NORMALIZE_REGEX_REPLACE_WHITESPACES_AND_QUOTES_BY_HYPHEN, NormalizationToolbox.NORMALIZE_REGEX_SHRINK_HYPHENS }),
    VOCABULARY(new String[] { NormalizationToolbox.NORMALIZE_REGEX_REPLACE_WHITESPACES_AND_QUOTES_BY_HYPHEN,
            NormalizationToolbox.NORMALIZE_REGEX_SHRINK_HYPHENS }),
    NOM(new String[] { NormalizationToolbox.NORMALIZE_REGEX_REMOVE_BRACKETS, NormalizationToolbox.NORMALIZE_REGEX_SHRINK_WHITESPACES,
            NormalizationToolbox.NORMALIZE_REGEX_GATHER_WORDS, NormalizationToolbox.NORMALIZE_REGEX_SHRINK_QUOTES_WITH_WS,
            NormalizationToolbox.NORMALIZE_REGEX_TRIM_SPECIAL_CHARACTERS,
            NormalizationToolbox.NORMALIZE_REGEX_REPLACE_WHITESPACES_AND_QUOTES_BY_HYPHEN, NormalizationToolbox.NORMALIZE_REGEX_SHRINK_HYPHENS }),
    NUXEO_ECM_NAME(new String[] { NormalizationToolbox.NORMALIZE_REGEX_REPLACE_SPECIAL_CHARACTERS_BY_HYPHENS,
            NormalizationToolbox.NORMALIZE_REGEX_TRIM_SPECIAL_CHARACTERS }),
    JSON(new String[] { NormalizationToolbox.NORMALIZE_REGEX_REPLACE_SPECIAL_QUOTE_MARK_BY_QUOTES,
            NormalizationToolbox.NORMALIZE_REGEX_SHRINK_QUOTES }),
    CIVILITE(new String[] { NormalizationToolbox.NORMALIZE_REGEX_CIVILITY_MALE, NormalizationToolbox.NORMALIZE_REGEX_CIVILITY_FEMALE }),
    WORD_ONLY(new String[] { NormalizationToolbox.NORMALIZE_REGEX_REMOVE_SPECIAL_CHARACTERS }),
    DEFAULT(new String[] { NormalizationToolbox.NORMALIZE_REGEX_REMOVE_BRACKETS, NormalizationToolbox.NORMALIZE_REGEX_SHRINK_WHITESPACES,
            NormalizationToolbox.NORMALIZE_REGEX_GATHER_WORDS });

    private final String[] rules;

    NormalizationPolicy(final String[] rules) {
        this.rules = rules;
    }

    @Override
    public String toString() {
        return rules.toString();
    }

    public String[] getRules() {
        return rules;
    }

}