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

public class LambertPoint {

    private double x;
    private double y;
    private double z;

    LambertPoint(double x, double y , double z){
        this.x = x;
        this.y = y;
        this.z = z;
    }
    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

   public double getY() {
        return y;
    }

   public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public void translate(double x , double y, double z){

        this.x+= x;
        this.y+= y;
        this.z+= z;
    }

    public LambertPoint toDegree(){
        this.x = this.x * 180/Math.PI;
        this.y = this.y * 180/Math.PI;
        this.z = this.z * 180/Math.PI;

        return this;
    }
}
