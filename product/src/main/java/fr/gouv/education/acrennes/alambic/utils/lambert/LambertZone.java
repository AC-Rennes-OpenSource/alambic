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
package fr.gouv.education.acrennes.alambic.utils.lambert;

public enum LambertZone{
	
	LambertI(0),LambertII(1),	LambertIII(2),	LambertIV(3),	LambertIIExtended(4),Lambert93(5);
	private final int lambertZone;

	private final static double[] LAMBERT_N = {0.7604059656, 0.7289686274, 0.6959127966, 0.6712679322, 0.7289686274, 0.7256077650};
	private final static double[] LAMBERT_C = {11603796.98, 11745793.39, 11947992.52, 12136281.99, 11745793.39, 11754255.426};
	private final static double[] LAMBERT_XS = {600000.0, 600000.0, 600000.0, 234.358, 600000.0, 700000.0};
	private final static double[] LAMBERT_YS = {5657616.674, 6199695.768, 6791905.085, 7239161.542, 8199695.768, 12655612.050};

    public final static double M_PI_2 = Math.PI/2.0;
    public final static double DEFAULT_EPS = 1e-10 ;
    public final static double E_CLARK_IGN =  0.08248325676  ;
    public final static double E_WGS84 =  0.08181919106  ;

    public final static double A_CLARK_IGN = 6378249.2 ;
    public final static double A_WGS84 =  6378137.0  ;
    public final static double LON_MERID_PARIS = 0  ;
    public final static double LON_MERID_GREENWICH =0.04079234433 ;
    public final static double LON_MERID_IERS = 3.0*Math.PI/180.0;



	private LambertZone(int value){
		this.lambertZone = value;
	}

	public double n(){
		return this.LAMBERT_N[this.lambertZone];
	}

	public  double c(){
		return this.LAMBERT_C[this.lambertZone];
	}
	public  double xs(){
		return this.LAMBERT_XS[this.lambertZone];
	}
	public  double ys(){
		return this.LAMBERT_YS[this.lambertZone];
	}

}