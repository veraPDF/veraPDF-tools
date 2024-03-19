package org.verapdf.tools;

import java.util.ArrayList;
import java.util.List;

public class DeprecatedFeatures {

	List<Long> procSet = new ArrayList<>();
	List<Long> CIDSet = new ArrayList<>();
	List<Long> charSet = new ArrayList<>();
	List<Long> name = new ArrayList<>();

	public List<Long> getProcSet() {
		return procSet;
	}

	public void setProcSet(List<Long> procSet) {
		this.procSet = procSet;
	}

	public List<Long> getCIDSet() {
		return CIDSet;
	}

	public void setCIDSet(List<Long> CIDSet) {
		this.CIDSet = CIDSet;
	}

	public List<Long> getCharSet() {
		return charSet;
	}

	public void setCharSet(List<Long> charSet) {
		this.charSet = charSet;
	}

	public List<Long> getName() {
		return name;
	}

	public void setName(List<Long> name) {
		this.name = name;
	}
}
