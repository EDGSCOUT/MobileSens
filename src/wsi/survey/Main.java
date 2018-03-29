package wsi.survey;


import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import wsi.mobilesens.MobileSens;
import wsi.mobilesens.R;
import wsi.survey.AnswerQuesion.PageTask;
import wsi.survey.CachedActivity.Node;
import wsi.survey.result.GConstant;
import wsi.survey.util.ImageAdapter;
import wsi.survey.util.ImageGallery;
import wsi.survey.util.PreferenceUtil;
import wsi.survey.util.UpdateManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main extends Activity {
	private final String TAG = "Main";
	private ImageGallery imgGallery;//首页底部图片gallery
	private TextView tvTitle;
	private TextView tvDescrp;
	private ImageView ivWelcome;
//	private Button resetButton;
	private RelativeLayout layout_main;
	
	private TelephonyManager mTelManager;
	
	private List<Map<String, Object>> imgList = new ArrayList<Map<String, Object>>();	// 问卷的图片资源
	private List<Map<String, String>> mList = new ArrayList<Map<String, String>>();		// 问卷的文件名、标题、描述

	private static boolean isFirstLoad = true;		// 是否是第一次加载
	private static boolean isStartActivity = false;		// 是否是开启新Activity
	public static final String NEW_LIFEFORM_DETECTED =  
	        "com.android.broadcast.NEW_LIFEFORM"; 
	UpdateManager mUpdateManager;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Log.i(TAG, "onCreate()");
		if(!isServiceRunning(MobileApplication.mContext, MobileSens.class.getName())){
			Log.e(Main.class.getSimpleName(), "service not running");
			Intent it = new Intent(NEW_LIFEFORM_DETECTED);           
	        sendBroadcast(it); 
		}		
		isFirstLoad = true;		
		initResource();		
		animPlay();
		
	}

	/** 初始化控件 */
	private void initResource(){
		ivWelcome = (ImageView)findViewById(R.id.ivWelcome);
		imgGallery = (ImageGallery)findViewById(R.id.imgGallery);
		tvTitle = (TextView)findViewById(R.id.tvCaption);
		tvDescrp = (TextView)findViewById(R.id.tvDescrp);
		layout_main = (RelativeLayout)findViewById(R.id.layout_main);
//		resetButton= (Button)findViewById(R.id.id_reset);
		mTelManager = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
		GConstant.IMEI = mTelManager.getDeviceId();
		
		// 获取物理屏幕
		DisplayMetrics dm = new DisplayMetrics();
		dm = getResources().getDisplayMetrics();
		int screenWidth = dm.widthPixels;
		int screenHeight = dm.heightPixels;
		GConstant.adjustFontSize(screenWidth, screenHeight);
		
		Log.i(TAG, "dm.density = " + dm.density + "; screenWidth = " + screenWidth + "; screenHeight = " + screenHeight);
	}
	
	/** 播放开机动画 */
	private void animPlay(){
		Animation anim = AnimationUtils.loadAnimation(Main.this, R.anim.anim_main_welcome);
		anim.setFillEnabled(true); 
		anim.setFillAfter(true);  
		ivWelcome.startAnimation(anim);		
		Message msg = Message.obtain();
		mHandler.sendMessageDelayed(msg, 3000);
	}
	
	private Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			ivWelcome.setVisibility(View.INVISIBLE);
			layout_main.setBackgroundResource(R.drawable.bg_main);
			checkForUpdate();
			loadResource();			// 开机动画后，开始加载资源并显示
		}
	};
	
	/** 加载资源，为控件赋值 */
	private void loadResource(){
		isFirstLoad = false;
		imgList.clear();
		mList.clear();
		for(int i=0; i<GConstant.surveyFiles.length; i++){
			Map<String, Object> imgMap = new HashMap<String, Object>();	// 图片资源
			boolean isCompleted=PreferenceUtil.getBooleanData(GConstant.surveyFiles[i][0]+"_completed");
			if(isCompleted){
				imgMap.put("images", GConstant.imgs_completed[i]);
			}else{
				imgMap.put("images", GConstant.imgs[i]);
			}
			imgList.add(imgMap);
			
			Map<String, String> map = new HashMap<String, String>();		// 文件名、标题、描述
			map.put("fileName", GConstant.surveyFiles[i][0]);
			map.put("fileTitle", GConstant.surveyFiles[i][1]);
			map.put("fileDescrp", GConstant.surveyFiles[i][2]);
			mList.add(map);
		}
		ImageAdapter imgAdapter = new ImageAdapter(this, imgList);
		
			GConstant.flags[0]=true;
		imgAdapter.createReflectedImages();
		imgGallery.setAdapter(imgAdapter);
		imgAdapter.notifyDataSetChanged();
		
		imgGallery.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				tvTitle.setTextSize(GConstant.titleFontSize);
				tvTitle.setText(GConstant.surveyFiles[position][1]);
				tvDescrp.setText(GConstant.surveyFiles[position][2]);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		
		imgGallery.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				
				/**如果某一问卷flag值设置为true，则表示该问卷已经达完，则可以进入该问卷，否则弹出警示*/
				boolean isCompleted_Last=true;
				if(position>0){
					isCompleted_Last=PreferenceUtil.getBooleanData(GConstant.surveyFiles[position-1][0]+"_completed");
				}
				if(GConstant.flags[position]||isCompleted_Last){
					Intent intent = new Intent(Main.this, AnswerQuesion.class);
					intent.putExtra("fileName", GConstant.surveyFiles[position][0]);
					isStartActivity = true;
					startActivity(intent);
				}else{
					Toast.makeText(Main.this, "请您按顺序依次填写问卷", Toast.LENGTH_LONG).show();
				}
			}
		});
	}

	@Override
	protected  void onStart(){
		super.onStart();
		Log.i(TAG, "onStart()");
	}
	
	
	public static int MENU_RESET = Menu.FIRST;
	public static int MENU_UPLOAD = Menu.FIRST + 1;
	public static int MENU_ABOUT = Menu.FIRST + 2;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		menu.add(0, MENU_RESET, 1, "全部重新填写");
		boolean isHaveCache=false;
		for(int i=0;i<GConstant.surveyFiles.length;i++){
			String value=PreferenceUtil.getData(GConstant.surveyFiles[i][0]);
			if(null!=value&&!"".equals(value)){
				isHaveCache=true;
				break;
			}
		}
		if(isHaveCache)
			menu.add(0,MENU_UPLOAD ,2, "再次上传未提交的结果");
		menu.add(0, MENU_ABOUT, 3, "关于");
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        if(item.getItemId() == MENU_RESET){
        	(new AlertDialog.Builder(Main.this).setTitle("提示")
                    .setMessage("确认要重新填写问卷？")
                    .setIcon(R.drawable.icon)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                        	for(int i=0;i<GConstant.surveyFiles.length;i++){
            					PreferenceUtil.clearBoolean(GConstant.surveyFiles[i][0]+"_completed");
            				}
            				loadResource();
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            //取消按钮事件
                        	
                        }
                    }))
                    .show();
        }
        else if(item.getItemId() == MENU_UPLOAD){
        	Intent intent=new Intent(Main.this,CachedActivity.class);
			startActivity(intent);
        }else if(item.getItemId() == MENU_ABOUT){
        	Intent intent=new Intent(Main.this,AboutActivity.class);
        	startActivity(intent);
        }
        return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume()");
		isStartActivity = false;
		
		if(!isFirstLoad){
			Log.i(TAG, "isFirstLoad = " + isFirstLoad);
			loadResource();
		} else {
			Log.i(TAG, "isFirstLoad = " + isFirstLoad);
		}		
	}

	@Override
	protected  void onPause() {
		super.onPause();
		Log.i(TAG, "onPause()");
		if(!isStartActivity){
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.i(TAG, "onStop()");
	}
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
		Log.i(TAG, "onDestroy()");
		Upload.apkVersion=null;
		Upload.apkDownLoadUrl=null;
	}
	
	private void checkForUpdate() {
		PageTask task = new PageTask(this);
		task.execute();
	}

	class PageTask extends AsyncTask<String, Integer, String> {
		// 可变长的输入参数，与AsyncTask.exucute()对应
		ProgressDialog progressDialog;

		public PageTask(Context context) {
			progressDialog = new ProgressDialog(context);
		}

		@Override
		protected String doInBackground(String... params) {
			Upload.doPostAll();
			return "";
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
			if(Upload.apkVersion!=null){
				PackageManager pm = getPackageManager();
				try{
					PackageInfo pInfo=pm.getPackageInfo(getPackageName(), PackageManager.GET_CONFIGURATIONS);
					String versionName = pInfo.versionName;					
					String nowVersion = versionName;
					String newVersion = Upload.apkVersion;
					if(newVersion.compareTo(nowVersion)>0){
				        mUpdateManager = new UpdateManager(Main.this);
				        mUpdateManager.checkUpdateInfo();
					}
				}catch (Exception e) {
					// TODO: handle exception
				}
			}else{
//				Toast.makeText(Main.this, "已是最新版", Toast.LENGTH_LONG).show();
			}
		
			
		}

		@Override
		protected void onPreExecute() {
//			progressDialog = ProgressDialog.show(Main.this, "正在检查更新……",
//					"请等待", true, false);
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			// 更新进度
		}

	}
	
	/**
     * 用来判断服务是否后台运行
     * @param context
     * @param className 判断的服务名字
     * @return true 在运行 false 不在运行
     */
    public static boolean isServiceRunning(Context mContext,String className) {
        boolean IsRunning = false;
        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE); 
        List<ActivityManager.RunningServiceInfo> serviceList   = activityManager.getRunningServices(30);
       if (!(serviceList.size()>0)) {
            return false;
        }
        for (int i=0; i<serviceList.size(); i++) {
            if (serviceList.get(i).service.getClassName().equals(className) == true) {
                IsRunning = true;
                break;
            }
        }
        return IsRunning ;
    }
}