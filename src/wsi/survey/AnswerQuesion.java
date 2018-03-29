package wsi.survey;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.http.util.EncodingUtils;
import org.json.JSONObject;

import wsi.mobilesens.R;
import wsi.survey.question.QuestionNaire;
import wsi.survey.question.QuestionXMLResolve;
import wsi.survey.result.GConstant;
import wsi.survey.util.PreferenceUtil;
import wsi.survey.util.Rotate3D;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class AnswerQuesion extends Activity {
	private final String TAG = "AnswerQuesion";

	private Rotate3D lQuest1Animation;
	private Rotate3D lQuest2Animation;
	private Rotate3D rQuest1Animation;
	private Rotate3D rQuest2Animation;
	private int mCenterX = 160;
	private int mCenterY = 0;

	private static final int MSG_LAYOUTA = 1; // 布局一
	private static final int MSG_LAYOUTB = 2; // 布局二（用于试题切换动画）

	private static final int MSG_START = 5; // 问卷开始测试试题（描述之后）
	private static final int MSG_NOCHOICE = 6; // 试题没有完成（无选项）

	private int currentIdx = -1; // 当前正是测试的试题下标（idx）

	public static String fileName; // 问卷文件名
	public static QuestionNaire qnNaire; // 问卷对应的问题实例
	private int currentQuestionNairId = -1;

	private LocationManager locationManager;
	
	private static final int ONE_SECOND = 1000;
	private static final int ONE_MINUTE = 60 * ONE_SECOND;

	private static final int MIN_GPS_TIME = 30 * ONE_MINUTE;
	private static final int MIN_GPS_DIST = 500;
	
	private long beginDate;
	private long endDate;
	
	private int animationDuration=700;

	/** 获取当前问卷的ID */
	public int getCurrentQuestionNairId() {
		return this.currentQuestionNairId;
	}

	/** 用于记录某一套题目的结果集 */
	List<HashMap<String, String>> results = new ArrayList<HashMap<String, String>>();

	/** 如果题目中有需要输入文字的题目，则显示该编辑框 */
	public EditText mEditText;

	/** 多选题目的容器 */
	LinearLayout mCheckBoxGroupLayout;
	CheckBox[] mCheckBox;
	List<HashMap<String, String>> hashMapList = new ArrayList<HashMap<String, String>>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate()");

		init();
	}

	private void init() {
		initAnimation();
		fileName = this.getIntent().getExtras().getString("fileName");
		findQuestionId();
		gotoQuestionNair(fileName);
		setGPS();
	       
	}
	
/*	*//**按屏幕比例调整各布局文件的比例*//*
	public void setLayout(){
		TextView tvCaption; 
		tvCaption=(TextView)findViewById(R.id.tvCaption);
		tvCaption.setLayoutParams(new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT,
				(int)(GConstant.deviceScreenHeight * 0.06f +0.5f)));
		
		LinearLayout layout_question,layout_question2;
		layout_question =(LinearLayout)findViewById(R.id.layout_question);
		layout_question.setLayoutParams(new LinearLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT,
				(int)(GConstant.deviceScreenHeight * 0.55f +0.5f)));
		layout_question2 =(LinearLayout)findViewById(R.id.layout_question2);
		layout_question2.setLayoutParams(new LinearLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT,
				(int)(GConstant.deviceScreenHeight * 0.55f +0.5f)));
		
		
		
	}*/
	/**获取GPS位置的函数*/
	private void setGPS(){
		String serviceName = Context.LOCATION_SERVICE;
        locationManager = (LocationManager) this.getSystemService(serviceName);
        // 查找到服务信息
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE); // 高精度
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW); // 低功耗

        String provider = locationManager.getBestProvider(criteria, true); // 获取GPS信息
        if(provider!=null){
	        Location location = locationManager.getLastKnownLocation(provider); // 通过GPS获取位置
	        updateToNewLocation(location);
	        // 设置监听器，自动更新的最小时间为间隔N秒(1秒为1*1000，这样写主要为了方便)或最小位移变化超过N米
	        locationManager.requestLocationUpdates(provider,  MIN_GPS_TIME, MIN_GPS_DIST,
		                gpsListenser);
        }
	}
	private void updateToNewLocation(Location location) {
		Log.e(AnswerQuesion.class.getSimpleName(), "updateToNewLocation:");
        if (location != null) {        	
            latitude = location.getLatitude();
            longitude= location.getLongitude();
//            Log.e(AnswerQuesion.class.getSimpleName(), "latitude:"+latitude);
        }

    }

	private void gotoQuestionNair(String filename) {
		fileName=filename;
		loadXMLFile(fileName); // 读取xml问卷，并解析xml
		goToDescription();
	}

	private void findQuestionId() {
		for (int i = 0; i < GConstant.surveyFiles.length; i++) {
			if (GConstant.surveyFiles[i][0].equals(fileName)) {
				currentQuestionNairId = i;
				break;
			}
		}
	}

	public void initAnimation() {
		
		lQuest1Animation = new Rotate3D(0, -90, 0, 0, mCenterX, mCenterY); // 下一题的【question1】旋转方向（从0度转到-90，参考系为水平方向为0度）
		lQuest1Animation.setFillAfter(true);
		lQuest1Animation.setDuration(animationDuration);

		lQuest2Animation = new Rotate3D(90, 0, 0, 0, mCenterX, mCenterY); // 下一题的【question2】旋转方向（从0度转到-90，参考系为水平方向为0度）（起始第一题）
		lQuest2Animation.setFillAfter(true);
		lQuest2Animation.setDuration(animationDuration);

		rQuest1Animation = new Rotate3D(0, 90, 0, 0, mCenterX, mCenterY); // 上一题的【question1】旋转方向（从0度转到90，参考系为水平方向为0度）
		rQuest1Animation.setFillAfter(true);
		rQuest1Animation.setDuration(animationDuration);

		rQuest2Animation = new Rotate3D(-90, 0, 0, 0, mCenterX, mCenterY); // 上一题的【question2】旋转方向（从-90度转到0，参考系为水平方向为0度）
		rQuest2Animation.setFillAfter(true);
		rQuest2Animation.setDuration(animationDuration);
	}

	/** 读取xml文件，并且解析xml */
	public void loadXMLFile(String fileName) {
		try {
			InputStream is = this.getResources().getAssets()
					.open(GConstant.surveyFileFolder + "/" + fileName);

			int len = is.available();
			byte[] buffer = new byte[len];
			is.read(buffer);

			String fileContent = EncodingUtils.getString(buffer, "utf-8");
			qnNaire = new QuestionNaire();
			// //////////////////////////////////////
			// xml parser
			// ///////////////////////////////////////
			QuestionXMLResolve.XML2QuestionNaire(qnNaire, fileContent);
			// //////////////////////////////////
			// ////////////////////////////
			// //////////////////////////////
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** 问卷的详细描述信息（info） */
	public void goToDescription() {
		setContentView(R.layout.descrip);

		TextView tvCaption = (TextView) findViewById(R.id.tvCaption); // catption
		tvCaption.setTextSize(GConstant.titleFontSize);
		tvCaption.setText(qnNaire.getCaption());

		TextView tvInfo = (TextView) findViewById(R.id.tvInfo); // info
		tvInfo.setText(qnNaire.getInfo());

		Button btnStartTest = (Button) findViewById(R.id.start); // start
		// test
		btnStartTest.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				beginDate=Calendar.getInstance().getTimeInMillis();
				Message msg = Message.obtain();
				msg.what = MSG_START;
				mHander.sendMessage(msg);
			}
		});
	}

	/** question xml layout */
	public void goToLayoutA(final int idx) {
		setContentView(R.layout.question);

		final LinearLayout layout_question = (LinearLayout) findViewById(R.id.layout_question); // main.xml
		// --
		// LinearLayout

		if (idx > currentIdx) {
			layout_question.startAnimation(lQuest2Animation);
			currentIdx = idx;
		} else {
			layout_question.startAnimation(rQuest2Animation);
			currentIdx = idx;
		}

		initLayout(idx, layout_question);
	}

	/** question2 xml layout */
	public void goToLayoutB(final int idx) {
		setContentView(R.layout.question2);

		final LinearLayout layout_question2 = (LinearLayout) findViewById(R.id.layout_question2);

		if (idx > currentIdx) {
			layout_question2.startAnimation(lQuest2Animation);
			currentIdx = idx;
		} else {
			layout_question2.startAnimation(rQuest2Animation);
			currentIdx = idx;
		}

		initLayout(idx, layout_question2);
	}

	private void saveDataToServer() {
		// 保存当前问卷结果到数据库中
//		Toast.makeText(this, "latitude:"+latitude+",longitude:"+longitude, Toast.LENGTH_LONG).show();
		endDate=Calendar.getInstance().getTimeInMillis();
		long timeSpan = (endDate-beginDate)/1000;
		SaveResult saveResult = new SaveResult(this,latitude,longitude,timeSpan);
		String result = saveResult.getAnswer();
		connectToServer(result);

	}

	/** 初始化布局文件 */
	private void initLayout(final int idx, final LinearLayout layout) {
		TextView tvCaption = (TextView) findViewById(R.id.tvCaption); // caption
		tvCaption.setTextSize(GConstant.titleFontSize);
		tvCaption.setText(qnNaire.getCaption());

		TextView tvQuestionTitle = (TextView) findViewById(R.id.tvQuestionTitle); // question
		// title
		tvQuestionTitle.setText((idx + 1) + ". "
				+ qnNaire.getQuestionItemTitle(idx));

		RadioGroup mRadioGroup = (RadioGroup) findViewById(R.id.rgOptionsGroup);
		mEditText = (EditText) findViewById(R.id.etContent);
		mCheckBoxGroupLayout = (LinearLayout) findViewById(R.id.checkBoxGroupLayout);

		int optionsNum = qnNaire.getQuestionItemOptionNum(idx); // 得到第idx道问题的选项个数
		final String type = qnNaire.getQuestionItemOptionType(idx);
		final RadioButton[] mRadioButton = new RadioButton[optionsNum]; // 根据选项个数，声明单选按钮数组
		mCheckBox = new CheckBox[optionsNum];

		if ("text".equals(type)) {
			mRadioGroup.setVisibility(View.GONE);
			mCheckBoxGroupLayout.setVisibility(View.GONE);
			mEditText.setVisibility(View.VISIBLE);
		} else if ("multi".equals(type)) {
			mRadioGroup.setVisibility(View.GONE);
			mCheckBoxGroupLayout.setVisibility(View.VISIBLE);
			mEditText.setVisibility(View.GONE);
			for (int i = 0; i < optionsNum; i++) {
				mCheckBox[i] = new CheckBox(this);
				mCheckBox[i].setId(i); // 设置id
				mCheckBox[i].setChecked(false);
				mCheckBox[i].setTextColor(Color.BLACK);
				mCheckBox[i].setText(qnNaire.getQuestionItemOptionText(idx, i));
				mCheckBoxGroupLayout.addView(mCheckBox[i], i);
			}
		} else {
			for (int i = 0; i < optionsNum; i++) {
				mRadioButton[i] = new RadioButton(this); // 创建单选按钮
				mRadioButton[i].setId(i); // 设置id
				mRadioButton[i].setChecked(false);
				mRadioButton[i].setTextColor(Color.BLACK);
				mRadioButton[i].setText(qnNaire.getQuestionItemOptionText(idx,
						i));
				mRadioGroup.addView(mRadioButton[i], i);
			}
			mRadioGroup.setVisibility(View.VISIBLE);
			mCheckBoxGroupLayout.setVisibility(View.GONE);
			mEditText.setVisibility(View.GONE);
		}

		Button btnPrevious = (Button) findViewById(R.id.btnPrevious); // 上一题
		Button btnNext = (Button) findViewById(R.id.btnNext); // 下一题

		if (idx == 0) {
			btnPrevious.setEnabled(false);// 第一题的上一题按键是无效的
		} else if (idx == (qnNaire.getQuestionsNum() - 1)
				&& (currentQuestionNairId < GConstant.surveyFiles.length - 1)) {
			btnNext.setText("下一问卷");
		} else if (idx == (qnNaire.getQuestionsNum() - 1)
				&& (currentQuestionNairId == GConstant.surveyFiles.length - 1)) {
			btnNext.setText("提交结果");
		}

		TextView tvQuestionsTotal = (TextView) findViewById(R.id.tvQuestionsTotal); // 题目总数
		TextView tvQuestionsRemain = (TextView) findViewById(R.id.tvQuestionsRemain); // 题目剩余数
		tvQuestionsTotal.setText("总共有" + qnNaire.getQuestionsNum() + "道题");
		tvQuestionsRemain.setText("还剩"
				+ (qnNaire.getQuestionsNum() - currentIdx - 1) + "道题");

		int oidx_selected = qnNaire.getQuestionItemAnswerOptionIndex(idx);
		if (oidx_selected >= 0) {
			mRadioButton[oidx_selected].setChecked(true);
		}

		mRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() { // RadioGroup选项监听
					@Override
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						int oidx_selected = checkedId;
						qnNaire.setQuestionItemAnswerOptionIndex(idx,
								oidx_selected);
					}
				});

		btnPrevious.setOnClickListener(new OnClickListener() { // 上一题的监听事件
					@Override
					public void onClick(View arg0) {
						if (idx != 0) {
							layout.startAnimation(rQuest1Animation);
							Message msg = Message.obtain();
							msg.what = MSG_LAYOUTA;
							msg.arg1 = idx - 1;
							mHander.sendMessage(msg);
						}
					}
				});

		btnNext.setOnClickListener(new OnClickListener() { // 下一题的监听事件
			@Override
			public void onClick(View arg0) {
				if (!isToNext(mRadioButton, mEditText)) {
					toastShow("请选择您的答案... ^_^");
					return;
				}
				if (mEditText != null
						&& mEditText.getVisibility() == View.VISIBLE) {
					qnNaire.setQuestionItemAnswerContent(idx, mEditText
							.getText().toString());
				} else if (mCheckBoxGroupLayout != null
						&& mCheckBoxGroupLayout.getVisibility() == View.VISIBLE) {
					for (int i = 0; i < mCheckBox.length; i++) {
						qnNaire.setQuestionItemAnswerOfN(idx,
								mCheckBox[i].isChecked(), i);
					}
				}
				// 当前问卷最后一题且当前问卷不是最后一个问卷
				if (idx == (qnNaire.getQuestionsNum() - 1)
						&& currentQuestionNairId < (GConstant.surveyFiles.length - 1)) {
					saveDataToServer();
					++currentQuestionNairId;
					GConstant.flags[currentQuestionNairId] = true;
					gotoQuestionNair(GConstant.surveyFiles[currentQuestionNairId][0]);
				} else if (idx == (qnNaire.getQuestionsNum() - 1)
						&& (currentQuestionNairId == GConstant.surveyFiles.length - 1)) {
					saveDataToServer(); // 最后一套问卷的最后一题					
				} else {
					layout.startAnimation(lQuest1Animation);
					Message msg = Message.obtain();
					msg.what = MSG_LAYOUTA;
					msg.arg1 = idx + 1;
					mHander.sendMessage(msg);
				}
			}
		});
	}

	private boolean isToNext(RadioButton[] mRadioButtons, EditText mEditText) {

		if (mEditText != null && mEditText.getVisibility() == View.VISIBLE) {
			if (mEditText.getText().toString().length() > 0) {
				return true;
			} else {
				return false;
			}

		} else if (mCheckBoxGroupLayout != null
				&& mCheckBoxGroupLayout.getVisibility() == View.VISIBLE) {
			for (int i = 0; i < mCheckBox.length; i++) {
				if (mCheckBox[i].isChecked()) {
					return true;
				}
			}
		} else {
			return isRadioChecked(mRadioButtons);
		}
		return false;
	}

	private boolean isRadioChecked(RadioButton[] mRadioButtons) {
		int childCount = mRadioButtons.length;

		for (int i = 0; i < childCount; i++) {
			if (mRadioButtons[i].isChecked()) {
				return true;
			}
		}
		return false;
	}

	//@Override
	/*public boolean onCreateOptionsMenu(Menu menu) {
		menu.clear();
		getMenuInflater().inflate(R.menu.answeroption, menu);
		return super.onCreateOptionsMenu(menu);
	}*/

	/*@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {

		switch (item.getItemId()) {
		case R.id.handin:
			submitResult();
			break;
		case R.id.cancel:
			finish();
			break;
		}

		return super.onMenuItemSelected(featureId, item);
	}*/

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			showDialog("调查问卷", "确定要退出吗？");
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	public void showDialog(String title, String msg) {
		AlertDialog dialog = new AlertDialog.Builder(this).setTitle(title)
				.setMessage(msg)
				.setPositiveButton("退出", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				})
				.setNegativeButton("取消", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				}).create();

		dialog.show();
	}

	private Handler mHander = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			switch (msg.what) {
			case MSG_START:
				goToLayoutA(0);
				break;
			case MSG_LAYOUTA:
				goToLayoutA(msg.arg1);
				break;
			case MSG_LAYOUTB:
				goToLayoutB(msg.arg1);
				break;
			case MSG_NOCHOICE:
				toastShow("请做出选择");
				break;
			}
		}
	};

	/** 没有选择任何选项，弹出提示选择 */
	private void toastShow(String msg) {
		Toast toast = Toast.makeText(AnswerQuesion.this, msg, 200);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}

/*	*//** 提交结果 *//*
	private void submitResult() {

		Intent intent = new Intent(AnswerQuesion.this, ShowResult.class);
		intent.putExtra("fileName", fileName);
		startActivity(intent);
	}*/

	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume()");
	}

	protected void onPause() {
		super.onPause();
		Log.i(TAG, "onPause()");
	}

	

	private void connectToServer(String value) {
		PageTask task = new PageTask(this);
		//Toast.makeText(this, value, Toast.LENGTH_LONG).show();
		task.execute(fileName, value, String.valueOf(currentQuestionNairId),qnNaire.getCaption());
	}

	class PageTask extends AsyncTask<String, Integer, String> {
		// 可变长的输入参数，与AsyncTask.exucute()对应
		ProgressDialog progressDialog;
		int currentId;
		String value;
		String filename;
		String caption;

		public PageTask(Context context) {
			progressDialog = new ProgressDialog(context);
		}

		@Override
		protected String doInBackground(String... params) {
			// return getData(params[0]);
			filename = params[0];
			value = params[1];
			currentId = Integer.valueOf(params[2]);
			caption=params[3];
			String result=Upload.doUpload(value);
			// 此处的返回值可以在onPostExecute的参数中接受
			// 主要是用来返回状态的，比如是否上传成功就可以在此处返回个onPostExecute,在onPostExecute中可以展示上传成功还是失败的结果了
			return result;
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
		}

		@Override
		protected void onPostExecute(String result) {
			// 此处的result参数是来自doInBackground的返回值
			if (progressDialog != null) {
				progressDialog.dismiss();
			}
			String valueString = "";
			if ("SUBMIT_OK".equals(result)) {
				// 上传成功
				valueString = "上传成功，进入下一问卷";
				if (currentId == GConstant.surveyFiles.length - 1) {
					valueString = "整套问卷已填写完毕并上传成功，感谢您的参与~~";
				}
				PreferenceUtil.saveData(filename+"_completed", true);
				PreferenceUtil.clear(filename);
			} else {
				// 其他上传失败
				// 尝试将数据加入缓存
				saveToPreference();
				valueString = "上传失败，结果已保存，您可以主界面菜单键中选择继续尝试上传，进入下一问卷";
				if (currentId == GConstant.surveyFiles.length - 1) {
					valueString = "上传失败，结果已保存，请在主界面继续尝试上传，感谢您的参与~";
				}
			}
			Toast.makeText(AnswerQuesion.this, valueString, Toast.LENGTH_LONG)
					.show();
			if(currentId == GConstant.surveyFiles.length - 1){
				Intent intent = new Intent(AnswerQuesion.this, Main.class);
				startActivity(intent);
				finish();// 结束当前activity
			}
		}

		@Override
		protected void onPreExecute() {
			progressDialog = ProgressDialog.show(AnswerQuesion.this, "上传中……",
					"请等待", true, false);
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			// 更新进度
		}

		private void saveToPreference() {
			PreferenceUtil.saveData(filename, value);
		}
	}

	private double latitude;
	private double longitude;
	LocationListener gpsListenser = new LocationListener() {

		public void onLocationChanged(Location arg0) {
			Log.i("position", "position has changed!");
			latitude = arg0.getLatitude();
			longitude = arg0.getLongitude();
		}

		public void onProviderDisabled(String arg0) {
		}

		public void onProviderEnabled(String arg0) {
		}

		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		}

	};
}
