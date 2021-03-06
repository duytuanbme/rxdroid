/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2013 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RxDroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RxDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package at.jclehner.rxdroid.ui;

import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import at.jclehner.rxdroid.BuildConfig;
import at.jclehner.rxdroid.DoseHistoryActivity;
import at.jclehner.rxdroid.DoseView;
import at.jclehner.rxdroid.DrugListActivity;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.Theme;
import at.jclehner.rxdroid.Version;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entries;
import at.jclehner.rxdroid.util.Constants;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.Extras;
import at.jclehner.rxdroid.util.Timer;
import at.jclehner.rxdroid.util.Util;
import at.jclehner.rxdroid.widget.DrugNameView;
import at.jclehner.rxdroid.widget.DrugSupplyMonitor;

public class DrugOverviewAdapter extends AbsDrugAdapter
{
	private static final String TAG = DrugOverviewAdapter.class.getSimpleName();
	private static final boolean LOGV = BuildConfig.DEBUG;

	private final Timer mTimer;

	public DrugOverviewAdapter(Activity activity, List<Drug> items, Date date)
	{
		super(activity, items, date);

		mTimer = LOGV ? new Timer() : null;
	}

	@Override
	public View getView(int position, View v, ViewGroup parent)
	{
		if(LOGV && position == 0)
			mTimer.restart();

		final Drug drug = getItem(position);
		final DoseViewHolder holder;

		if(v == null)
		{
			v = mActivity.getLayoutInflater().inflate(R.layout.drug_view, null);

			holder = new DoseViewHolder();

			holder.name = (DrugNameView) v.findViewById(R.id.drug_name);
			holder.icon = (ImageView) v.findViewById(R.id.drug_icon);
			holder.missedDoseIndicator = (ImageView) v.findViewById(R.id.img_missed_dose_warning);
			//holder.log = (ImageView) v.findViewById(R.id.img_drug_menu);
			holder.historyMenu = v.findViewById(R.id.frame_history_menu);
//			holder.lowSupplyIndicator = v.findViewById(R.id.low_supply_indicator);
			holder.currentSupply = (DrugSupplyMonitor) v.findViewById(R.id.text_supply);

			for(int i = 0; i != holder.doseViews.length; ++i)
			{
				final int doseViewId = Constants.DOSE_VIEW_IDS[i];
				holder.doseViews[i] = (DoseView) v.findViewById(doseViewId);
				mActivity.registerForContextMenu(holder.doseViews[i]);
			}

			holder.setDividersFromLayout(v);

			v.setTag(holder);
		}
		else
			holder = (DoseViewHolder) v.getTag();

		//holder.name.setUnscrambledText(drug.getName());
		holder.name.setDrug(drug);
		holder.name.setTag(DrugListActivity.TAG_DRUG_ID, drug.getId());

		//holder.icon.setImageResource(drug.getIconResourceId());
		holder.icon.setImageResource(Util.getDrugIconDrawable(getContext(), drug.getIcon()));
		holder.currentSupply.setDrugAndDate(drug, mAdapterDate);

		final Date today = DateTime.today();
		final boolean isCurrentSupplyVisible;
		boolean isMissingDoseIndicatorVisible = false;

		if(today.equals(mAdapterDate))
		{
			if(Entries.hasMissingDosesBeforeDate(drug, mAdapterDate))
				isMissingDoseIndicatorVisible = true;

			isCurrentSupplyVisible = drug.getRefillSize() != 0 || !drug.getCurrentSupply().isZero();
		}
		else
			isCurrentSupplyVisible = mAdapterDate.after(today);

		holder.missedDoseIndicator.setVisibility(isMissingDoseIndicatorVisible ? View.VISIBLE : View.GONE);
		holder.historyMenu.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(mActivity, DoseHistoryActivity.class);
				intent.putExtra(Extras.DRUG_ID, drug.getId());
				mActivity.startActivity(intent);
			}
		});

		holder.currentSupply.setVisibility(isCurrentSupplyVisible ? View.VISIBLE : View.INVISIBLE);

		for(DoseView doseView : holder.doseViews)
		{
			if(!doseView.hasInfo(mAdapterDate, drug))
				doseView.setDoseFromDrugAndDate(mAdapterDate, drug);
		}

		final int dividerVisibility;
		if(v.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
			dividerVisibility = View.GONE;
		else
			dividerVisibility = View.VISIBLE;

		for(int i = 0; i != holder.dividers.length; ++i)
		{
			final View divider = holder.dividers[i];
			if(divider != null)
				divider.setVisibility(dividerVisibility);
		}

		if(LOGV && position == getCount() - 1)
		{
			final double elapsed = mTimer.elapsedSeconds();
			final int viewCount = getCount() * 4;
			final double timePerView = elapsed / viewCount;

			Log.v(TAG, mAdapterDate + ": " + viewCount + " views created in " + elapsed + "s (" + timePerView + "s per view)");
		}

		if(BuildConfig.DEBUG)
		{
			//holder.dividers[1].setVisibility(View.VISIBLE);
			//holder.dividers[2].setVisibility(View.VISIBLE);
		}

		return v;
	}
}
