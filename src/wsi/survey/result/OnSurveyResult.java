package wsi.survey.result;


public interface OnSurveyResult {

	/** 计算每道试题的得分（第idx到试题，选项为answer，得分以逆序、顺序计分而不同） */
	public int calQuestionItems(int idx, int answer);			

	/** 计算问卷总分（如果结果有多个维度，则以字符串返回，多个结果之间用逗号","隔开） */
	public String getSurveyResultTotal(String answerScore);		
	
	/** 计算问卷标准分（返回结果形式与总分相同） */
	public String getSurveyResultStd();		
	
	/** 返回标准分对应的评判结果 */
	public String getSurveyResultRemark();		

	/** 返回评判结果对应的维度名 */
	public String getSurveyResultTagTitle();		
	
	/** float数组转换为String字符串 */
	public String float2String(float []array);	
	
}

