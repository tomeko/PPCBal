package com.falro;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class PPCBal extends Activity {
	private Dialog dialog;
	private Button btn_update, btn_exit, btn_okay;
	private String login, password, plan, min_remain, txt_remain, data_remain, bal_remain, bal_exp;
	private boolean bad_lp, no_lp;
	Editor editor;
	EditText txtLogin, txtPassword;
	CheckBox savepw;
	ProgressDialog waiting;
	SharedPreferences sharedPreferences;
	
@Override
public void onPause() {
    super.onPause();
    save(savepw.isChecked());	
}

@Override
public void onResume() {
    super.onResume();	
    savepw.setChecked(load());
}

@Override
public void onStart() {
    super.onStart();
    savepw.setChecked(load());
}
private void save(final boolean isChecked) {
	sharedPreferences = getPreferences(Context.MODE_PRIVATE);
	SharedPreferences.Editor editor = sharedPreferences.edit();
	editor.putBoolean("pwcheck", isChecked);
	if (isChecked){
		// then save the username and pw
		editor.putString("login", txtLogin.getText().toString());
		editor.putString("password", txtPassword.getText().toString());
	}
	editor.commit();
	}

// when the program loads, check whether user selected to remember login info, if so, populate it
private boolean load() {
	sharedPreferences = getPreferences(Context.MODE_PRIVATE);
    if (sharedPreferences.getBoolean("pwcheck",false)){
    	txtLogin.setText(this.getPreferences(MODE_PRIVATE).getString("login", ""));
    	txtPassword.setText(this.getPreferences(MODE_PRIVATE).getString("password", ""));
    }
	return sharedPreferences.getBoolean("pwcheck", false);
}

// function to check whether there is online connectivity
public boolean isOnline() {
	final ConnectivityManager conMgr = (ConnectivityManager) getSystemService (Context.CONNECTIVITY_SERVICE);
    if (conMgr.getActiveNetworkInfo() != null && conMgr.getActiveNetworkInfo().isAvailable() && conMgr.getActiveNetworkInfo().isConnected()) {
    	return true;
    } else {
    	return false;
    }
}
	
// handler that shows dialog when it's ready
private Handler handler = new Handler() {
	@Override
	public void handleMessage(Message msg) {
		dialog.show();
        }
};

// exit, but check if user check save user info
private OnClickListener exit = new OnClickListener() {
    public void onClick(View v) {
    	save(savepw.isChecked());
    	finish();
    }
};
	
private OnClickListener dialogDismiss = new OnClickListener() {
    public void onClick(View v) {
    	dialog.dismiss();
    }
};   

private OnClickListener getBalance = new OnClickListener() {
    public void onClick(View v) {
    	if (isOnline()) {
            waiting = ProgressDialog.show(PPCBal.this, "", "Connecting. Please wait...", true);
            new Thread(new Runnable(){  
         	   public void run() {
         		   getFields();
         	       handler.sendEmptyMessage(0);
         	       waiting.dismiss();
         	      }
         	   }).start();    
    	}
    	else {
    		Toast.makeText(PPCBal.this, "No Network detected. Please connect to continute.", Toast.LENGTH_SHORT).show();
    	}}
    
};

// this is the parser that gets retrieves all the balance info
private void getFields(){
		bad_lp = no_lp = false;
		login = txtLogin.getText().toString();
		password = txtPassword.getText().toString();
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();  
        nameValuePairs.add(new BasicNameValuePair("username", login));  
        nameValuePairs.add(new BasicNameValuePair("password", password));
        HttpResponse response = null;
        HttpParams params = new BasicHttpParams(); // setup whatever params you what
        HttpClient client = new DefaultHttpClient(params);
        HttpPost post = new HttpPost("https://www.pagepluscellular.com/login.aspx");
        HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000);
        HttpConnectionParams.setSoTimeout(client.getParams(), 10000);
		BufferedReader br = null;
        try {
			post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			response = client.execute(post);
			br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()), 8096);
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		} catch (ClientProtocolException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} 
		String line = null;
		try {
			while ((line = br.readLine()) != null) {
			    // System.out.println(line); // useful for debugging
			    if (line.indexOf("lblBalance") != -1) {
					bal_remain = line.substring(line.indexOf("$"), line.indexOf("<", line.indexOf("$")));
					
					//System.out.println(line.substring(line.indexOf("$"), line.indexOf("<", line.indexOf("$"))));
					}
				if (line.indexOf("lblBundleName") != -1) {
					plan = line.substring(line.indexOf("<b>")+3, line.indexOf("</b>", line.indexOf("$")));
					//System.out.println(line.substring(line.indexOf("<b>")+3, line.indexOf("</b>", line.indexOf("$"))));
					}
				if (line.indexOf("lblBundleValue") != -1) {
					String [] tmp  = line.substring(line.indexOf(">")+1, line.indexOf("</s", line.indexOf("$"))).split(" ");
					min_remain = tmp[0];
					txt_remain = tmp[1];
					data_remain = tmp[2];
					//System.out.println(line.substring(line.indexOf(">")+1, line.indexOf("</s", line.indexOf("$"))));
					}
				if (line.indexOf("Invalid") != -1) {
					bad_lp = true;
					}
				if (line.indexOf("does") != -1) {
					no_lp = true;
					}
				}
				
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// set all the balance info text
		Button oak = (Button)dialog.findViewById(R.id.okay);
		oak.setOnClickListener(dialogDismiss);
		TextView baln = (TextView) dialog.findViewById(R.id.bal);
        baln.setText(bal_remain);
        TextView txtplan = (TextView) dialog.findViewById(R.id.plan);
        txtplan.setText(plan);
        TextView min_left = (TextView) dialog.findViewById(R.id.min);
        min_left.setText(min_remain);
        TextView txt_left = (TextView) dialog.findViewById(R.id.txt);
        txt_left.setText(txt_remain);
        TextView data_left = (TextView) dialog.findViewById(R.id.data);
        data_left.setText(data_remain);
        TextView mesg = (TextView) dialog.findViewById(R.id.msg);
        
        // handle bad login responses
        if (bad_lp){
        	 mesg.setText("Incorrect Username or Password!");
        } else if (no_lp) {
       	 mesg.setText("No such username!");
        } else if (bal_remain == null && data_remain == null) {
        	 mesg.setText("Error connecting to PagePlusCellular.com!");  
        } else {
        	mesg.setText("");
        }
}

	
@Override
public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    
    btn_update = (Button)findViewById(R.id.btnupdate);
    btn_exit = (Button)findViewById(R.id.btnexit);
    
    savepw = (CheckBox)findViewById(R.id.remember);
    txtLogin = (EditText)findViewById(R.id.log);
    txtPassword = (EditText)findViewById(R.id.pass);
    dialog = new Dialog(PPCBal.this);
    dialog.setContentView(R.layout.balance);
    dialog.setTitle("Your Balance:");	
    dialog.setCancelable(true);
    
    btn_update.setOnClickListener(getBalance);
    btn_exit.setOnClickListener(exit);
    }

}