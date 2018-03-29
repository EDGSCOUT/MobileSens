package wsi.survey.question;


public class QuestionItem {
	private String qid;			// 问题编号number，如 17道问题，即 1~17
	private String tag;
	private String questionType;
	private String title;		// 问题题目content
	private OptionItem []optionItems;
	private int oidx;
	private int oidx_selected;
	
	private int optionNum;

	private boolean[] answerMoption;	// 用于多选项的 checked
	private String addedContent;  //用于填写内容checked

	public QuestionItem() {
		this.oidx = 0;
		this.oidx_selected = -1;
	}

	public void setQid(String qid) {
		this.qid = qid;
	}

	public String getQid(){
		return this.qid;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getTag(){
		return this.tag;
	}

	public void setQuestionType(String questionType) {
		this.questionType = questionType;
	}

	public String getQuestionType(){
		return this.questionType;
	}
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setOptionsNum(String questionType, int optionNum) {
		this.optionItems = new OptionItem[optionNum];	
		
		if(questionType.equals("multi")){	// checked 多选
			this.answerMoption = new boolean[optionNum];
			for (int i = 0; i < optionNum; i++) {
				answerMoption[i] = false;
			}
		}
	}

	public void setOptionItem(int oidx, OptionItem optionItem) {
		this.optionItems[oidx] = optionItem;
		this.oidx = oidx;
	}

	public OptionItem getOptionItem(int oidx) {
		if(oidx >= 0) {
			return optionItems[oidx];
		}
		return null;
	}

	public int getOptionItemIndex() {
		return oidx;
	}

	public int getOptionItemNum() {
		this.optionNum = Math.max(0, oidx+1);
		return this.optionNum;
	}

	public void setAnswerOptionIndex(int oidx_selected) {
		this.oidx_selected = oidx_selected;
	}
	
	public int getAnswerOptionIndex() {
		return this.oidx_selected;
	}
	
	public void setAnswerContent(String value){
		this.addedContent=value;
	}
	
	public String getAnswerContent(){
		return this.addedContent;
	}

	public void setAnswerOfM(boolean answer, int oidx) {
		this.answerMoption[oidx] = answer;
	}

	public boolean getAnswerofM(int oidx) {
		return this.answerMoption[oidx];
	}

}
