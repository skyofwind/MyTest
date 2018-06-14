package com.example.anthero.myapplication;

import android.app.Dialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

public class BaseActvity extends AppCompatActivity {
    //定时器相关
    private Dialog progressDialog;
    private boolean  progress=false;


    public void statrProgressDialog(String text){
        if(progressDialog == null){
            progressDialog = new Dialog(this,R.style.progress_dialog);
            progressDialog.setContentView(R.layout.dialog);
            progressDialog.setCancelable(false);
            progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            TextView msg = (TextView) progressDialog.findViewById(R.id.id_tv_loadingmsg);
            msg.setText(text);
        }
        progress=true;
        progressDialog.show();
    }
    public void cancel(){
        if(progress){
            progress=false;
            progressDialog.dismiss();
            Log.d("cancel", " cancel");
        }
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        cancel();
    }
}
