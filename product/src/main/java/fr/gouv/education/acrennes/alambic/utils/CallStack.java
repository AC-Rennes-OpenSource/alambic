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
package fr.gouv.education.acrennes.alambic.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CallStack {

	private String id;
	private String param;
	private String fulltext;
	private final List<CallStack> stack;
	private final Map<String, String> ctx;

	public CallStack(final String id) {
		setId(id);
		stack = new ArrayList<>();
		ctx = new HashMap<String, String>();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public String getParam() {
		return param;
	}

	public void setParam(final String param) {
		this.param = param;
	}

	public String getFulltext() {
		return fulltext;
	}

	public void setFulltext(final String fulltext) {
		this.fulltext = fulltext;
	}

	public List<CallStack> getStack() {
		return stack;
	}

	public Map<String, String> getCtx() {
		return ctx;
	}

	public void add(final CallStack stack) {
		getStack().add(stack);
	}

	public CallStack getInner(final String fulltext) {
		CallStack inner = null;
		for (CallStack item : getStack()) {
			if (item.getFulltext().equals(fulltext)) {
				inner = item;
				break;
			}
		}
		return inner;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		boolean status = false;

		if (null != obj) {
			status = getFulltext().equals(((CallStack) obj).getFulltext());
		}

		return status;
	}

	@Override
	public String toString() {
		return "{\"id\":\"" + id + "\",\"fulltext\":\"" + fulltext + "\",\"param\":\"" + param + "\",\"stack\":[" + callStackToString() + "]}";
	}

	private String callStackToString() {
		String str = "";
		if (!getStack().isEmpty()) {
			for (CallStack item : getStack()) {
				str = str + item + ",";
			}
			str = str.replaceAll(",$", "");
		}
		return str;
	}

}
