package com.disappointedpig.dpmidi;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.disappointedpig.midi.MIDIDebugEvent;
import com.disappointedpig.midi.MIDISession;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

//import android.support.design.widget.FloatingActionButton;
//import android.support.design.widget.Snackbar;
//import android.support.v7.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {

    static class ViewHolder {
        TextView text1;
        TextView text2;
        int position;
    }

    private MIDIService midiServiceRef;
    private ServiceConnection midiServiceConnection;
    SharedPreferences sharedpreferences;

    ArrayList<MIDIDebugEvent> activityList=new ArrayList<MIDIDebugEvent>();

    ArrayAdapter<MIDIDebugEvent> adapter;

    private MIDISession midiSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sharedpreferences = getSharedPreferences("DPMIDIPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putBoolean("MIDIState", false);
        editor.commit();

        EventBus.getDefault().register(this);
        startMIDIService();

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

//        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, activityList);

        adapter = new ArrayAdapter<MIDIDebugEvent>(this,R.layout.twolinelistrow,activityList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
//                ViewHolder holder;
                if (convertView == null) {
                    // You should fetch the LayoutInflater once in your constructor
                    LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    convertView = inflater.inflate(R.layout.twolinelistrow, null);
                }
                    // Initialize ViewHolder here
//                } else {
//                    holder = (ViewHolder) convertView.getTag();
//                }
//        File file = filesArrayList.get(position);
                MIDIDebugEvent d=activityList.get(position);
//                holder.text1.setText(d.module);
//                holder.text2.setText(d.message);
//                Log.d("ma","module: "+d.module+" message: "+d.message);
                TextView v = (TextView) convertView.findViewById(R.id.text1);
                v.setText(d.module);
                v = (TextView) convertView.findViewById(R.id.text2);
                v.setText(d.message);
                return convertView;
            }
        };
        ListView listView = (ListView) findViewById(R.id.activity_listview);
        listView.setAdapter(adapter);

        ToggleButton midiToggle = (ToggleButton) findViewById(R.id.midiToggleButton);
        ToggleButton midiServiceToggle = (ToggleButton) findViewById(R.id.midiServiceToggleButton);


        midiToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
//                    updateList("midi on");
                    startupMIDI();
                } else {
//                    updateList("midi off");
                    shutdownMIDI();
                }
            }
        });

        midiServiceToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
//                    updateList("midi preference on");
//                    startupMIDI();
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putBoolean("MIDIState", true);
                    editor.commit();

                } else {
//                    updateList("midi preference off");
//                    shutdownMIDI();
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putBoolean("MIDIState", false);
                    editor.commit();
                }
            }
        });

        Button testMIDIButton = (Button) findViewById(R.id.testMIDIButton);
        testMIDIButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendTestMIDI();
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMIDIDebugEvent(final MIDIDebugEvent event) {
//        Toast.makeText(this, "got midi event", Toast.LENGTH_SHORT).show();
//        Log.d("ahs", "got midi event");
//        final Context context = this;
//        Handler h = new Handler(Looper.getMainLooper());
//        h.post(new Runnable() {
//            public void run() {
//                Toast.makeText(context, "got midi event", Toast.LENGTH_SHORT).show();
//            }
//        });
        updateList(event);
    }


    private void updateList(MIDIDebugEvent d) {
        activityList.add(d);
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void startupMIDI() {
        Log.d("MIDISession","startupMIDI");
        midiSession = MIDISession.getInstance();
        midiSession.initMIDI(this,10);
        midiSession.startListening();
    }

    public void shutdownMIDI() {
        midiSession.stopListening();
    }

    public void startMIDIService() {
        Intent cmsIntent = new Intent(this,MIDIService.class);
        startService(cmsIntent);

        midiServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                // Called when the connection is made.
                midiServiceRef = ((MIDIService.MIDIServiceBinder) service).getService();

            }

            public void onServiceDisconnected( ComponentName className) {
                // Received when the service unexpectedly disconnects.
                midiServiceRef = null;
            }
        };

        Intent bindintent = new Intent(this,MIDIService.class);
        bindService(bindintent, midiServiceConnection, Context.BIND_AUTO_CREATE);

    }

    public void sendTestMIDI() {
//        MIDIMessage message = MIDISession.getInstance().sendNote(41,127);
//        if(message != null) {
            MIDISession.getInstance().sendNote(41,127);
//        }
    }

}


