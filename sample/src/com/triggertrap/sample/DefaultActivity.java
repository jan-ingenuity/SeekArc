/*******************************************************************************
 * The MIT License (MIT)
 * 
 * Copyright (c) 2013 Triggertrap Ltd
 * Author Neil Davies
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ******************************************************************************/
package com.triggertrap.sample;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.triggertrap.seekarc.SeekArc;


/**
 * 
 * DefaultActivity.java
 * @author Neil Davies
 * 
 */
public class DefaultActivity extends Activity {

	protected SeekArc mSeekArc;
	protected SeekBar mInnerBounds;
	protected SeekBar mOuterBounds;
	protected CheckBox mRoundedEdges;
	protected CheckBox mEnabled;
	protected Button mCommit;
	protected TextView mSeekArcProgress;
	protected TextView mInnerBoundsLabel;
	protected TextView mOuterBoundsLabel;

	protected int getLayoutFile(){
		return R.layout.sample__default;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(getLayoutFile());
		
		mSeekArc = (SeekArc) findViewById(R.id.seekArc);
		mSeekArcProgress = (TextView) findViewById(R.id.seekArcProgress);
		mInnerBounds = (SeekBar) findViewById(R.id.innerBounds);
		mInnerBoundsLabel = (TextView) findViewById(R.id.innerBoundsLabel);
		mOuterBounds = (SeekBar) findViewById(R.id.outerBounds);
		mOuterBoundsLabel = (TextView) findViewById(R.id.outerBoundsLabel);
		mRoundedEdges = (CheckBox) findViewById(R.id.roundedEdges);
		mEnabled = (CheckBox) findViewById(R.id.enabled);
		mCommit = (Button) findViewById(R.id.commit);

		SeekBarListener barListener = new SeekBarListener();
		CheckboxListener checkboxListener = new CheckboxListener();

		float density = this.getResources().getDisplayMetrics().density;
		int bounds;

		bounds = (int)((float)mSeekArc.getInnerBounds() / density);
		mInnerBounds.setProgress(bounds);
		mInnerBoundsLabel.setText(String.valueOf(bounds));

		bounds = (int)((float)mSeekArc.getOuterBounds() / density);
		mOuterBounds.setProgress(bounds);
		mOuterBoundsLabel.setText(String.valueOf(bounds));

		mSeekArc.setOnSeekArcChangeListener(new SeekArcListener());
		mInnerBounds.setOnSeekBarChangeListener(barListener);
		mOuterBounds.setOnSeekBarChangeListener(barListener);

		mRoundedEdges.setOnCheckedChangeListener(checkboxListener);
		mRoundedEdges.setChecked(mSeekArc.isRoundedEdges());
		mEnabled.setOnCheckedChangeListener(checkboxListener);
		mEnabled.setChecked(mSeekArc.isEnabled());
		mCommit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mSeekArc.setValue(mSeekArc.getValue());
			}
		});
	}

	protected class SeekArcListener implements SeekArc.OnSeekArcChangeListener {
		@Override
		public void onStopTrackingTouch(SeekArc seekArc) {
		}

		@Override
		public void onStartTrackingTouch(SeekArc seekArc) {
		}

		@Override
		public void onProgressChanged(SeekArc seekArc, int progress, boolean fromUser) {
			mSeekArcProgress.setText(String.valueOf(progress));
		}
	}

	protected class SeekBarListener implements SeekBar.OnSeekBarChangeListener {
		@Override
		public void onStopTrackingTouch(SeekBar arg0) {

		}

		@Override
		public void onStartTrackingTouch(SeekBar arg0) {
		}

		@Override
		public void onProgressChanged(SeekBar view, int progress, boolean fromUser) {
			float density = DefaultActivity.this.getResources().getDisplayMetrics().density;

			progress = (int)(progress * density);

			if (view.getId() == R.id.innerBounds) {
				mInnerBoundsLabel.setText(String.valueOf(progress));
				mSeekArc.setInnerBounds(progress);
			}
			else if (view.getId() == R.id.outerBounds) {
				mOuterBoundsLabel.setText(String.valueOf(progress));
				mSeekArc.setOuterBounds(progress);
			}

			mSeekArc.requestLayout();
			mSeekArc.invalidate();
		}
	}

	protected class CheckboxListener implements CompoundButton.OnCheckedChangeListener {
		@Override
		public void onCheckedChanged(CompoundButton view, boolean isChecked) {
			if (view.getId() == R.id.enabled)
				mSeekArc.setEnabled(isChecked);
			else if (view.getId() == R.id.roundedEdges)
				mSeekArc.setRoundedEdges(isChecked);

			mSeekArc.invalidate();
		}
	}
}
