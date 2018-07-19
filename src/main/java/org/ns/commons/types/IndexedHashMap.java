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
package org.ns.commons.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

public class IndexedHashMap<X extends Identifiable> implements Serializable {
	
	private static final long serialVersionUID = 749279547724847039L;
	private HashMap<X, Integer> _map;
	private List<X> _list;
	private boolean _locked;
	
	public IndexedHashMap(){
		this._map = new HashMap<X,Integer>();
		this._list = new ArrayList<X>();
		this._locked = false;
	}
	
	public boolean isLocked(){
		return this._locked;
	}
	
	public void lock(){
		this._locked = true;
	}
	
	public int add(X x){
		if(_locked){
			throw new IllegalStateException("This map is locked, no new elements can be added.");
		}
		if(this._map.containsKey(x)){
			return this._map.get(x);
		}
		int id = this._list.size();
		x.setId(id);
		this._map.put(x, id);
		this._list.add(x);
		return id;
	}
	
	public int getId(Object identifier){
		Integer result = this._map.get(identifier);
		if(result == null){
			throw new NoSuchElementException("The identifier "+identifier+" of type "+identifier.getClass().getName()+" is not found.");
		}
		return result;
	}
	
	public X get(int id){
		return this._list.get(id);
	}
}
