/** Statistical Natural Language Processing System
    Copyright (C) 2014-2016  Lu, Wei

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * 
 */
package org.ns.commons.types;

import java.io.Serializable;

/**
 * @author wei_lu
 */
public class Label implements Serializable, Comparable<Label>, Identifiable {
	
	private static final long serialVersionUID = -5006849791095171763L;
	
	private String _form;
	private int _id;
	
	public Label(Label lbl){
		this._form = lbl._form;
		this._id = lbl._id;
	}
	
	public Label(String form, int id){
		this._form = form;
		this._id = id;
	}
	
	public void setId(int id){
		this._id = id;
	}
	
	public int getId(){
		return this._id;
	}
	
	public String getForm(){
		return this._form;
	}
	
	public boolean equals(Object o){
		if(o instanceof Label){
			Label l = (Label)o;
			return this._form.equals(l._form);
		} else if(o instanceof String){
			return this._form.equals(o);
		}
		return false;
	}
	
	public int hashCode(){
		return _form.hashCode();
	}
	
	public String toString(){
		return _form;
	}

	@Override
	public int compareTo(Label o) {
		return Integer.compare(_id, o._id);
	}
	
}
