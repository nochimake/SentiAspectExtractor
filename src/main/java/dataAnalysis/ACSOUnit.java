package dataAnalysis;

public class ACSOUnit {
	private int[] aspect;
	private int[] opinion;
	private int sentiment;
	private String catagory;
	public ACSOUnit(int[] aspect, String catagory, int sentiment, int[] opinion) {
		super();
		this.aspect = aspect;
		this.opinion = opinion;
		this.sentiment = sentiment;
		this.catagory = catagory;
	}
	public ACSOUnit(int[] aspect, int sentiment) {
		this(aspect,null,sentiment,null);
	}
	public int[] getAspect() {
		return aspect;
	}
	public void setAspect(int[] aspect) {
		this.aspect = aspect;
	}
	public int[] getOpinion() {
		return opinion;
	}
	public void setOpinion(int[] opinion) {
		this.opinion = opinion;
	}
	public int getSentiment() {
		return sentiment;
	}
	public void setSentiment(int sentiment) {
		this.sentiment = sentiment;
	}
	public String getCatagory() {
		return catagory;
	}
	public void setCatagory(String catagory) {
		this.catagory = catagory;
	}
	

}
