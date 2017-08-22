package com.sprocomm.ofoscenetest;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.sprocomm.ofoscenetest.utils.ContastValue;
import com.sprocomm.ofoscenetest.utils.PrefUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class SettingsActivity extends Activity implements View.OnClickListener {

    @InjectView(R.id.et_time)
    EditText etTime;
    @InjectView(R.id.et_ip)
    EditText etIp;
    @InjectView(R.id.et_port)
    EditText etPort;
    @InjectView(R.id.save_all)
    Button saveAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.inject(this);
        etIp.setHint(PrefUtils.getString(this, ContastValue.PREF_IP,ContastValue.IP));
        etPort.setHint(PrefUtils.getInt(this,ContastValue.PREF_PORT,ContastValue.PORT)+"");
        etTime.setHint(PrefUtils.getInt(this,ContastValue.PREF_TIME,0)+"");
        saveAll.setOnClickListener(this);
    }

    private boolean isGoodIp(String ip){
        String rexp = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";

        Pattern pat = Pattern.compile(rexp);
        Matcher mat = pat.matcher(ip);
        return mat.find();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.save_all:
                if(etIp.getText().length() > 6) {
                    final String ip = etIp.getText().toString();
                    if(isGoodIp(ip)) {
                        PrefUtils.setString(this,ContastValue.PREF_IP,ip);
                        etIp.setText(null);
                        etIp.setHint(ip+"");
                    }else{
                        Toast.makeText(this, "请输入正确的ip", Toast.LENGTH_SHORT).show();
                    }
                }

                if(etPort.getText().length() > 0) {
                    final String portstr = etPort.getText().toString();
                    final int port = Integer.parseInt(portstr);
                    PrefUtils.setInt(this,ContastValue.PREF_PORT, port);
                    etPort.setHint(port+"");
                    etPort.setText(null);
                }
                if(etTime.getText().length() > 0) {
                    final String timestr = etTime.getText().toString();
                    final int time = Integer.parseInt(timestr);
                    PrefUtils.setInt(this,ContastValue.PREF_TIME, time);
                    etTime.setText(null);
                    etTime.setHint(time+"");
                }
                finish();
                break;
        }

    }
}
