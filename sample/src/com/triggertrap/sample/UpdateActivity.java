package com.triggertrap.sample;


import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.triggertrap.seekarc.SeekArc;


/**
 * Created by aeroheart-c6 on 25/03/2017.
 */
public class UpdateActivity extends Activity {
    private SeekArc arc;
    private EditText progressET;
    private EditText deltaET;
    private EditText valueET;
    private Button progressB;
    private Button deltaB;
    private Button valueB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.sample__update);

        this.arc = (SeekArc)this.findViewById(R.id.seekArc);

        this.progressET = (EditText)this.findViewById(R.id.progress);
        this.deltaET = (EditText)this.findViewById(R.id.delta);
        this.valueET = (EditText)this.findViewById(R.id.value);

        ClickListener listener = new ClickListener();

        this.progressB = (Button)this.findViewById(R.id.progress__btn);
        this.progressB.setOnClickListener(listener);
        this.deltaB = (Button)this.findViewById(R.id.delta__btn);
        this.deltaB.setOnClickListener(listener);
        this.valueB = (Button)this.findViewById(R.id.value__btn);
        this.valueB.setOnClickListener(listener);
    }


    private class ClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            UpdateActivity self = UpdateActivity.this;

            switch (view.getId()) {
                case R.id.progress__btn:
                    self.arc.setProgress(Integer.parseInt(self.progressET.getText().toString()));
                    break;

                case R.id.delta__btn:
                    self.arc.setDelta(Integer.parseInt(self.deltaET.getText().toString()));
                    break;

                case R.id.value__btn:
                    self.arc.setValue(Integer.parseInt(self.valueET.getText().toString()));
                    break;
            }
        }
    }
}
