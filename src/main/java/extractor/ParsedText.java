package extractor;

import java.util.ArrayList;

import edu.stanford.nlp.ling.IndexedWord;

public class ParsedText {
	private String inputText;
	private boolean isParsed;
	private String text;
	private ArrayList<Opinion> opinion_list;
	private ArrayList< ArrayList<Aspect> > aspectListOfOpinion;
	public ParsedText(String inputText, boolean isParsed, String text, ArrayList<Opinion> opinion_list,ArrayList<ArrayList<Aspect>> aspectListOfOpinion) {
		super();
		this.inputText = inputText;
		this.isParsed = isParsed;
		this.text = text;
		this.opinion_list = opinion_list;
		this.aspectListOfOpinion = aspectListOfOpinion;
	}
	public ParsedText(String inputText, boolean isParsed) {
		this(inputText,isParsed,null,null,null);
	}
	public String getInputText() {
		return inputText;
	}
	public boolean isParsed() {
		return isParsed;
	}
	public String getText() {
		return text;
	}
	public ArrayList<Opinion> getOpinion_list() {
		return opinion_list;
	}
	public ArrayList<ArrayList<Aspect>> getAspectListOfOpinion() {
		return aspectListOfOpinion;
	}
}
