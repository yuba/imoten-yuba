/*
 * imoten - i mode.net mail tensou(forward)
 * 
 * Copyright (C) 2010 shoozhoo (http://code.google.com/p/imoten/)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
 */

package immf;

public class Emoji {
	private char c;
	private String label;
	private String googleImage;
	
	public Emoji(char c, String label,String googleImage){
		this.c = c;
		this.label = label;
		this.googleImage = googleImage;
	}
	
	public char getC() {
		return c;
	}
	public String getLabel() {
		return label;
	}
	public String getgoogleImage() {
		return googleImage;
	}
	
	
}
