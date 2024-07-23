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
 * NORMALIZATION TOOL BOX
 * --------------------------------------------------------------------------------------------------------
 */
public interface NormalizationToolbox {

    /**
     * Remove supernumerary contiguous quotes. Example: "l'''avion" becomes "l'avion"
     */
    String NORMALIZE_REGEX_SHRINK_QUOTES = "'{2,}='";

    /**
     * Remove supernumerary whitespaces associated to quotes. Example: "l '   avion" becomes "l'avion"
     * Remove supernumerary contiguous quotes. Example: "l'''avion" becomes "l'avion"
     */
    String NORMALIZE_REGEX_SHRINK_QUOTES_WITH_WS = "\\s*'+\\s*='";

    /**
     * Remove supernumerary whitespaces associated to quotes. Example: "l '   avion" becomes "lavion"
     * Remove quotes. Example: "l'''avion" becomes "lavion"
     */
    String NORMALIZE_REGEX_REMOVE_QUOTES = "\\s*'+\\s*=";

    /**
     * Remove useless starting non-word characters. Example: "+++l'avion" becomes "l'avion"
     * Remove useless ending non-word characters. Example: "l'avion'" becomes "l'avion"
     */
    String NORMALIZE_REGEX_TRIM_SPECIAL_CHARACTERS = "^\\W+|\\W+$=";

    /**
     * Replace quotes by hyphen character. Example: "l'avion" becomes "l-avion"
     * Replace whitespaces by hyphen character. Example: "le matin" becomes "le-matin"
     */
    String NORMALIZE_REGEX_REPLACE_WHITESPACES_AND_QUOTES_BY_HYPHEN = "[\\s']+=-";

    /**
     * Replace supernumerary hyphen characters. Example: "l---avion" becomes "l-avion"
     */
    String NORMALIZE_REGEX_SHRINK_HYPHENS = "-{2,}=-";

    /**
     * Remove brackets. Example: "les oiseaux (bleus)" becomes "les oiseaux bleus"
     */
    String NORMALIZE_REGEX_REMOVE_BRACKETS = "\\(|\\)=";

    /**
     * Remove supernumerary whitespaces. Example: "les    oiseaux" becomes "les oiseaux"
     */
    String NORMALIZE_REGEX_SHRINK_WHITESPACES = "(\\S+)\\s{2,}(\\S+)=$1 $2";

    /**
     * Gather separated words. Example: "plate -   forme" becomes "plate-forme"
     */
    String NORMALIZE_REGEX_GATHER_WORDS = "(\\w+)\\s*([^\\w\\s]+)\\s*(\\w+)=$1$2$3";

    /**
     * Replace non-word characters by hyphen. Example: "Les 3: (graces) " becomes "Les-3-graces"
     */
    String NORMALIZE_REGEX_REPLACE_SPECIAL_CHARACTERS_BY_HYPHENS = "\\W+=-";

    /**
     * Replace non-word characters by hyphen. Example: "Les 3: (graces) " becomes "Les-3-graces"
     */
    String NORMALIZE_REGEX_REMOVE_SPECIAL_CHARACTERS = "[\\W_]+=";

    /**
     * Replace quotation marks by simple quotes. Example: "Le livre "Le rouge et le noir" est de Stendhal" becomes "Le livre 'Le rouge et le noir'
     * est de Stendhal"
     */
    String NORMALIZE_REGEX_REPLACE_SPECIAL_QUOTE_MARK_BY_QUOTES = "\"='";

    /**
     * Replace female civility by normalised one. Example: "Mm" & "Melle" becomes "Mme"
     */
    String NORMALIZE_REGEX_CIVILITY_FEMALE = "^[Mm][Ll][Ll][Ee].*|^[Mm][Mm][Ee]?.*|^[Mm][Ee].*|^[Mm][Aa][Dd][Aa][Mm][Ee]" +
                                             ".*|^[Mm][Aa][Dd][Ee][Mm][Oo][Ii][Ss][Ee][Ll][Ll][Ee].*=Mme";

    /**
     * Replace male civility by normalised one. Example: "Mr" & "Monsieur" becomes "M"
     */
    String NORMALIZE_REGEX_CIVILITY_MALE = "^m$|^[Mm][Rr].*|^[Mm]\\..*|^[Mm][Oo][Nn][Ss][Ii][Ee][Uu][Rr].*=M";

}
