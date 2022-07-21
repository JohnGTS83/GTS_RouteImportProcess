package com.kaldin.dto;

public class RunUpdateStatusDTO {

	private int runid;
	private boolean newStatus;
	private boolean stopsUpdated;
	private boolean oldUpdate;
	private String componentVariation;
	
	public String getComponentVariation() {
		return componentVariation;
	}
	public void setComponentVariation(String componentVariation) {
		this.componentVariation = componentVariation;
	}

	public int getRunid() {
		return runid;
	}
	public void setRunid(int runid) {
		this.runid = runid;
	}
	public boolean isNewStatus() {
		return newStatus;
	}
	public void setNewStatus(boolean newStatus) {
		this.newStatus = newStatus;
	}

	public boolean isStopsUpdated() {
		return stopsUpdated;
	}
	public void setStopsUpdated(boolean stopsUpdated) {
		this.stopsUpdated = stopsUpdated;
	}
	public boolean isOldUpdate() {
		return oldUpdate;
	}
	public void setOldUpdate(boolean oldUpdate) {
		this.oldUpdate = oldUpdate;
	}
	public String toString() {
		return runid + " " + newStatus + " : " + stopsUpdated;
	}
}