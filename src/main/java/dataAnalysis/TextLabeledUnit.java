package dataAnalysis;

import java.util.ArrayList;

public class TextLabeledUnit {
	private String text;
	private ArrayList<ACSOUnit> acso_list;
	public TextLabeledUnit(String text, ArrayList<ACSOUnit> acso_list) {
		super();
		this.text = text;
		this.acso_list = acso_list;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public ArrayList<ACSOUnit> getAcso_list() {
		return acso_list;
	}
	public void setAcso_list(ArrayList<ACSOUnit> acso_list) {
		this.acso_list = acso_list;
	}
}
