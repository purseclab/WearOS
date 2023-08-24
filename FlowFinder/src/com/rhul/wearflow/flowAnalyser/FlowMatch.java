package com.rhul.wearflow.flowAnalyser;

import org.json.simple.JSONObject;

public class FlowMatch {

	public static int total = 0;
	protected JSONObject mobile;
	protected JSONObject wear;
	protected String direction;
	protected int id;

	public FlowMatch(JSONObject mobile, JSONObject wear, String direction) {
		this.mobile = mobile;
		this.wear = wear;
		this.direction = direction;
		total = total + 1;
		this.id = total;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return this.id;
	}

	public JSONObject getMobile() {
		return mobile;
	}

	public void setMobile(JSONObject mobile) {
		this.mobile = mobile;
	}

	public JSONObject getWear() {
		return wear;
	}

	public void setWear(JSONObject wear) {
		this.wear = wear;
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	@Override
	public String toString() {
		FlowUtil util = FlowUtil.getInstance();
		String output = null;
		if (direction.equals("mobile-wear"))
			output = "\n[Flow direction = " + direction + "-" + id + "\nMobile flow = " + util.formatFlow(mobile)
					+ "\nWear flow=" + util.formatFlow(wear) + "]";
		else
			output = "\n[Flow direction=" + direction + "-" + id + "\n  Wear flow=" + util.formatFlow(wear)
					+ "\nMobile flow=" + util.formatFlow(mobile) + "]";
		return output;
	}

}