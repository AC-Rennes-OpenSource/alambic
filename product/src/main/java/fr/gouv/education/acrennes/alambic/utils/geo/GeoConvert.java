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
package fr.gouv.education.acrennes.alambic.utils.geo;

import org.apache.log4j.Logger;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.util.FactoryException;

public class GeoConvert {
	private static Logger logger = Logger.getLogger(GeoConvert.class);
	final CoordinateReferenceSystem targetCRS = CommonCRS.WGS84.geographic();

	final CoordinateOperation lambert93ToWgs84;
	/** La Réunion */
	final CoordinateOperation rgr92ToWgs84;
	final CoordinateOperation guadeloupeToWgs84;
	final CoordinateOperation guyaneToWgs84;
	final CoordinateOperation mayotteToWgs84;
	final CoordinateOperation stPierreEtMiquelonToWgs84;

	/**
	 * Récupère le système qui permet de convertir des coordonnées géographiques en fonction du référentiel propre à chaque département. Tous les département de Métropole ont le
	 * même référentiel {@link #getLambert93ToWgs84()}
	 *
	 * @param codeDepartement
	 *            code du département sur 3 chiffres
	 * @return par défaut {@link #getLambert93ToWgs84()}
	 */
	public CoordinateOperation getOperationFromCodeDepartement(String codeDepartement) {
		switch (codeDepartement) {
		case "971":
		case "972":
			// Martinique et Guadeloupe :
			return guadeloupeToWgs84;
		case "973":
			return guyaneToWgs84;
		case "974":
			return rgr92ToWgs84;
		case "975":
			return stPierreEtMiquelonToWgs84;
		case "976":
			return mayotteToWgs84;
		default:
			return lambert93ToWgs84;
		}
	}

	public GeoConvert() {
		logger.info("Initialisation du convertisseur de coordonnées géographiques...");
		try {
		// RGF93 / Lambert-93
		final CoordinateReferenceSystem lambert93 = CRS.forCode("EPSG:2154");
		lambert93ToWgs84 = CRS.findOperation(lambert93, targetCRS, null);

		// La Réunion - EPSG:2975 RGR92 / UTM zone 40S
		final CoordinateReferenceSystem rgr92 = CRS.forCode("EPSG:2975");
		rgr92ToWgs84 = CRS.findOperation(rgr92, targetCRS, null);

		// Guadeloupe - WSG84 / UTM zone 20N
		final CoordinateReferenceSystem guadeloupe = CommonCRS.WGS84.universal(15, -63);
		guadeloupeToWgs84 = CRS.findOperation(guadeloupe, targetCRS, null);

		// Guyane - RGFG95 - UTM Nord fuseau 22, aussi référencé sous EPSG:2972
		final CoordinateReferenceSystem guyane = CommonCRS.WGS84.universal(4, (22 * 6) - 183); // -53
		guyaneToWgs84 = CRS.findOperation(guyane, targetCRS, null);

		// Mayotte - UTM Sud fuseau 38
		final CoordinateReferenceSystem mayotte = CommonCRS.WGS84.universal(-12, 45);
		mayotteToWgs84 = CRS.findOperation(mayotte, targetCRS, null);

		// Saint Pierre et Miquelon - UTM Nord fuseau 21
		final CoordinateReferenceSystem stPierreEtMiquelon = CommonCRS.WGS84.universal(46, -56);
		stPierreEtMiquelonToWgs84 = CRS.findOperation(stPierreEtMiquelon, targetCRS, null);

		// Aucune données géographique dans Ramses, ni sur http://www.education.gouv.fr/acce_public
		// Nouvelle Calédonie - RGNC91-93
		// Wallis et Futuna
		// Polynésie
		} catch (final FactoryException ex) {
			logger.error("problème à l'initialisation du convertisseur de coordonnées géographiques.", ex);
			throw new GeoConvertInitException(ex);
		}
		logger.info("Initialisation du convertisseur de coordonnées géographiques terminé.");
	}

	public CoordinateOperation getLambert93ToWgs84() {
		return lambert93ToWgs84;
	}

	/**
	 * La Réunion - EPSG:2975 RGR92 / UTM zone 40S
	 */
	public CoordinateOperation getRgr92ToWgs84() {
		return rgr92ToWgs84;
	}

	/**
	 * Guadeloupe - WSG84 / UTM zone 20N
	 */
	public CoordinateOperation getGuadeloupeToWgs84() {
		return guadeloupeToWgs84;
	}

	/**
	 * Guyane - RGFG95 - UTM Nord fuseau 22, aussi référencé sous EPSG:2972
	 */
	public CoordinateOperation getGuyaneToWgs84() {
		return guyaneToWgs84;
	}

	/**
	 * Mayotte - UTM Sud fuseau 38
	 */
	public CoordinateOperation getMayotteToWgs84() {
		return mayotteToWgs84;
	}

	/**
	 * Saint Pierre et Miquelon - UTM Nord fuseau 21
	 */
	public CoordinateOperation getStPierreEtMiquelonToWgs84() {
		return stPierreEtMiquelonToWgs84;
	}


}
