package wsi.survey.question;


public class OptionItem {
	
	private String aid;
	private String score;
	private String optionType;
	private String text;
	
	public OptionItem(){
		this.aid = "";
		this.score = "";
		this.text = "";
	}
	
	public OptionItem(int oidx, String aid, String score, String text) {
		this.aid = aid;
		this.score = score;
		this.text = text;
	}
	
	public void setAid(String aid) {
		this.aid = aid;
	}
	
	public String getAid() {
		return this.aid;
	}

	public void setScore(String score) {
		this.score = score;
	}
	
	public String getScore() {
		return this.score;
	}

	public void setOptionType(String optionType) {
		this.optionType = optionType;
	}
	
	public String getOptionType() {
		return this.optionType;
	}

	public void setText(String text) {
		this.text = text;
	}
	
	public String getText() {
		return this.text;
	}
}