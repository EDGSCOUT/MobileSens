package wsi.survey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import wsi.mobilesens.R;
import wsi.survey.AnswerQuesion.PageTask;
import wsi.survey.result.GConstant;
import wsi.survey.util.PreferenceUtil;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class CachedActivity extends Activity {

	ListView mListView;
	Button uploadButton;
	Handler myHandler;
	int sucessNum=0;
	int failNum=0;
	class Node {
		private String filename;
		private String value;
		private String caption;

		public String getCaption() {
			return caption;
		}

		public void setCaption(String caption) {
			this.caption = caption;
		}

		public String getFilename() {
			return filename;
		}

		public void setFilename(String filename) {
			this.filename = filename;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	};
	
	

	List<Node> cachedList = new ArrayList<CachedActivity.Node>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.cached);
		mListView = (ListView) findViewById(R.id.id_cached_listview);
		uploadButton = (Button) findViewById(R.id.id_upload);
		uploadButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				uploadButton.setEnabled(false);
				uploadToServer();
			}
		});
		showinList();
		
		
		myHandler = new Handler(){
			public void handleMessage(android.os.Message msg) {
				String message="";
				if(msg.what==1){
					 message="上传成功";
					 Toast.makeText(CachedActivity.this, message, Toast.LENGTH_LONG).show();
					 Intent intent=new Intent(CachedActivity.this,Main.class);
					 startActivity(intent);
					 finish();
				}else{
					message="上传失败";
					Toast.makeText(CachedActivity.this, message, Toast.LENGTH_LONG).show();
				}				
				uploadButton.setEnabled(true);
				/*
				if(msg.what==1){
					sucessNum++;
				}else{
					failNum++;
				}
				int allNum=sucessNum+failNum;
				if(allNum==cachedList.size()){
					String message="";
					if(sucessNum==0){
						message="上传失败";
					}else if(failNum==0){
						 message="上传成功";
						 Intent intent=new Intent(CachedActivity.this,Main.class);
						 startActivity(intent);
						 finish();
					}else{
						message="上传成功:"+sucessNum+"个，上传失败:"+failNum+"个";
					}
					Toast.makeText(CachedActivity.this, message, Toast.LENGTH_LONG).show();
					uploadButton.setEnabled(true);
				}
				*/
			};
		};
		
	}
	
	

	private void showinList() {
		getCachedList();
		if (cachedList.size() == 0) {
			uploadButton.setVisibility(View.GONE);
		} else {
			uploadButton.setVisibility(View.VISIBLE);
			showInView();
		}
	}

	private void getCachedList() {
		for (int i = 0; i < GConstant.surveyFiles.length; i++) {
			String value = PreferenceUtil.getData(GConstant.surveyFiles[i][0]);
			if (null != value && !"".equals(value)) {
				Node newNode = new Node();
				newNode.setFilename(GConstant.surveyFiles[i][0]);
				newNode.setCaption(GConstant.surveyFiles[i][1]);
				newNode.setValue(value);
				if (!cachedList.contains(newNode))
					cachedList.add(newNode);
			}
		}
	}

	private void showInView() {
		ArrayList<HashMap<String, Object>> listItem = new ArrayList<HashMap<String, Object>>();
		for (int i = 0; i < cachedList.size(); i++) {
			Node node = cachedList.get(i);
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("ItemName", node.getCaption());
			// map.put("ItemContent", node.getValue());
			listItem.add(map);
		}
		// 生成适配器的Item和动态数组对应的元素
		SimpleAdapter listItemAdapter = new SimpleAdapter(this, listItem,// 数据源
				R.layout.cached_item,// ListItem的XML实现
				// 动态数组与ImageItem对应的子项
				new String[] { "ItemName" },
				// ImageItem的XML文件里面的一个ImageView,两个TextView ID
				new int[] { R.id.id_filename });

		// 添加并且显示
		mListView.setAdapter(listItemAdapter);

	}

	private void uploadToServer() {
//		for(int i=0;i<cachedList.size();i++){
//			connectToServer(cachedList.get(i));
//		}
		JSONArray jsonArray=new JSONArray();
		for(int i=0;i<cachedList.size();i++){
			JSONObject jsonObject;
			try {
				jsonObject = new JSONObject(cachedList.get(i).getValue());
				jsonArray.put(jsonObject);
			} catch (Exception e) {
				// TODO: handle exception
			}			
		}
//		connectToServer(cachedList.get(i));
		connectToServer();
	}

	private void connectToServer() {
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
			if(cachedList.size()>1){
				JSONArray jsonArray=new JSONArray();
				for(int i=0;i<cachedList.size();i++){
					JSONObject jsonObject;
					try {
						jsonObject = new JSONObject(cachedList.get(i).getValue());
						jsonArray.put(jsonObject);
					} catch (Exception e) {
						// TODO: handle exception
					}			
				}
				Log.e(CachedActivity.class.getSimpleName(), jsonArray.toString());
				return Upload.doUpload(jsonArray.toString());
			}else if(cachedList.size()==1){
				JSONObject jsonObject;
				try {
					jsonObject = new JSONObject(cachedList.get(0).getValue());
					return Upload.doUpload(jsonObject.toString());
				} catch (Exception e) {
					// TODO: handle exception
					return "";
				}				
			}else{
				return "";
			}
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
			if ("SUBMIT_OK".equals(result)) {
				// 上传成功
				clearToPreference();
				Message msg=new Message();
				msg.what=1;
				myHandler.sendMessage(msg);
				for(int i=0;i<cachedList.size();i++){
					PreferenceUtil.saveData(cachedList.get(i).getFilename()+"_completed", true);
				}				
				cachedList.clear();
			} else {
				// 其他上传失败
				Message msg=new Message();
				msg.what=0;
				myHandler.sendMessage(msg);
			}

		}

		@Override
		protected void onPreExecute() {
			progressDialog = ProgressDialog.show(CachedActivity.this, "上传中……",
					"请等待", true, false);
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			// 更新进度
		}
		
		private void clearToPreference() {
			for(int i=0;i<cachedList.size();i++){
				PreferenceUtil.clear(cachedList.get(i).getFilename());
			}
		}

	}
}
