package com.marspotato.supportsmallshop;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.joda.time.DateTime;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.gson.JsonSyntaxException;
import com.marspotato.supportsmallshop.R;
import com.marspotato.supportsmallshop.BO.Shop;
import com.marspotato.supportsmallshop.adaptor.ShopListAdapter;
import com.marspotato.supportsmallshop.util.Config;
import com.marspotato.supportsmallshop.util.RequestManager;

import android.location.Location;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ShopListActivity extends Activity implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener 
{
	private LocationClient mLocationClient;
	private Shop[] shopList;
	private int selectedDistrict;
	private DateTime lastClickTime;//Just for avoiding double-click problem, no need to persistence
	
	
	/*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle dataBundle) {
    	//on purposely show nothing, to avoid confusion to end user.
    }
    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
    	//on purposely show nothing, to avoid confusion to end user.
    }
    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    	//NICE: make this part better
        Toast.makeText(this, this.getString(R.string.location_service_error_message), Toast.LENGTH_LONG).show();
    }

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putString("shopListJSON", Config.defaultGSON.toJson(shopList) );
		savedInstanceState.putInt("selectedDistrict", selectedDistrict );
	}

	private void setupTabColor(int viewId, boolean isHighLighted) {
		TextView t = (TextView) findViewById(viewId);
		t.setBackgroundColor(getResources().getColor(isHighLighted ? R.color.title_pale_grey : R.color.title_dark_grey));
		t.setTextColor(isHighLighted ? Color.WHITE : getResources().getColor(R.color.tab_dim_text_color));
	}
	public void filterbyGPSAction(View v)
	{
		setupTabColor(R.id.whole_city_tab, false);
		setupTabColor(R.id.hk_island_tab, false);
		setupTabColor(R.id.kowloon_tab, false);
		setupTabColor(R.id.new_territories_tab, false);
		setupTabColor(R.id.gps_tab, true);

		selectedDistrict = Config.WHOLE_HK;
		
	    Location location = mLocationClient.getLastLocation();
	    if (location == null)
	    {
	    	//NICE: make this part better
			Intent intent = new Intent(ShopListActivity.this, ShowGenericErrorActivity.class);
			intent.putExtra("message", getString(R.string.location_error_message));
			startActivity(intent);
	    }
	    else
	    {
	    	int latitude1000000 = (int) (location.getLatitude() * 1000000);
	    	int longitude1000000 = (int) (location.getLongitude() * 1000000);
	    	getShopList(latitude1000000, longitude1000000);
	    }
	}
	public void regionFilterAction(View v) {		
		setupTabColor(R.id.whole_city_tab, v.getId() == R.id.whole_city_tab);
		setupTabColor(R.id.hk_island_tab, v.getId() == R.id.hk_island_tab);
		setupTabColor(R.id.kowloon_tab, v.getId() == R.id.kowloon_tab);
		setupTabColor(R.id.new_territories_tab, v.getId() == R.id.new_territories_tab);
		setupTabColor(R.id.gps_tab, false);
		
		switch (v.getId()) {
			case R.id.whole_city_tab:
				selectedDistrict = Config.WHOLE_HK;
				break;
			case R.id.hk_island_tab:
				selectedDistrict = Config.HK_ISLAND;
				break;
			case R.id.kowloon_tab:
				selectedDistrict = Config.KOWL0ON;
				break;
			case R.id.new_territories_tab:
				selectedDistrict = Config.NEW_TERRITORIES;
				break;
		}
		getShopList();
	}

	@Override
    protected void onStart() {
        super.onStart();

        mLocationClient.connect();
    }
    @Override
    protected void onStop() {

        mLocationClient.disconnect();
        super.onStop();
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.shop_list);
		mLocationClient = new LocationClient(this, this, this);
		
		if (savedInstanceState != null) {
			selectedDistrict = savedInstanceState.getInt("selectedDistrict");
			String shopListJSON = savedInstanceState.getString("shopListJSON");
			shopList = (Shop[]) Config.defaultGSON.fromJson(shopListJSON, Shop[].class);

			//resume the display
			ListView shopListView = (ListView) findViewById(R.id.shop_list);
			shopListView.setAdapter(new ShopListAdapter(this, shopList));
			findViewById(R.id.shop_list).setVisibility(View.VISIBLE);
			findViewById(R.id.progress_bar).setVisibility(View.GONE);
		} else {
			selectedDistrict = Config.WHOLE_HK;
			getShopList();
		}

		EditText edittext = (EditText) findViewById(R.id.search_word);
		edittext.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// If the event is a key-down event on the "enter" button
				if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
					ShopListActivity.this.getShopList();
					return true;
				}
				return false;
			}
		});

		Spinner spinner = (Spinner) findViewById(R.id.shop_type_spinner);
		// Create an ArrayAdapter using the string array and a default spinner layout
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.shop_type_display_values , android.R.layout.simple_spinner_item);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				ShopListActivity.this.getShopList();
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				ShopListActivity.this.getShopList();
			}} );

		// Click event for single list row
		ListView shopListView = (ListView) findViewById(R.id.shop_list);
		shopListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View v, int pos, long id) {
				if (lastClickTime != null && lastClickTime.plusMillis(Config.AVOID_DOUBLE_CLICK_PERIOD).isAfterNow())
					return;
				lastClickTime = DateTime.now();

				Intent intent = new Intent(ShopListActivity.this, ShopDetailActivity.class);
				intent.putExtra("shop", shopList[pos]);
				startActivity(intent);

			}
		});
	}

	/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	*/
	private void getShopList()
	{
	    getShopList(-1, -1);
	}
	private void getShopList(int latitude1000000, int longitude1000000) {
		// onPreExecute
		findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
		findViewById(R.id.main_layout).setVisibility(View.GONE);

		Response.Listener<String> listener = new Response.Listener<String>() {
			@Override
			public void onResponse(String response) {
				try {
					Shop[] result = Config.defaultGSON.fromJson(response, Shop[].class);
					ShopListActivity.this.shopList = result;
					ListView shopListView = (ListView) findViewById(R.id.shop_list);
					shopListView.setAdapter(new ShopListAdapter(ShopListActivity.this, result));
				} catch (JsonSyntaxException ex) {
					// failed json parsing means the network is already hijacked
					Intent intent = new Intent(ShopListActivity.this, ShowGenericErrorActivity.class);
					intent.putExtra("message", getString(R.string.network_redirection_error_message));
					startActivity(intent);
				}
				findViewById(R.id.progress_bar).setVisibility(View.GONE);
				findViewById(R.id.main_layout).setVisibility(View.VISIBLE);
			}
		};

		Response.ErrorListener errorListener = new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				if ((error instanceof NetworkError) || (error instanceof NoConnectionError) || (error instanceof TimeoutError)) {
					Intent intent = new Intent(ShopListActivity.this, ShowGenericErrorActivity.class);
					intent.putExtra("message", getString(R.string.network_connection_error_message));
					startActivity(intent);
					return;
				}
			}
		};

		String url = "";
		try {
			url = Config.HOST_URL + "/Shop?district=" + selectedDistrict;
			
			Spinner spinner = (Spinner) findViewById(R.id.shop_type_spinner);
			int selectedType = spinner.getSelectedItemPosition();
			if (selectedType != Spinner.INVALID_POSITION && selectedType > 0)
				url = url + "&shopType=" + URLEncoder.encode(Config.shopTypes[selectedType-1], "UTF-8");
			
			
			if (latitude1000000 > 0 && longitude1000000 > 0)
				url = url + "&range=" + Config.DEFAULT_SEARCH_RANGE + "&latitude1000000=" + latitude1000000 + "&longitude1000000=" + longitude1000000;

			TextView t = (TextView) findViewById(R.id.search_word);
			String searchWord = t.getText().toString();
			if (!searchWord.isEmpty())
				url = url + "&searchWord=" + URLEncoder.encode(searchWord, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// should never reach this line
			e.printStackTrace();
		}
		StringRequest request = new StringRequest(Request.Method.GET, url, listener, errorListener);
		request.setRetryPolicy(new DefaultRetryPolicy(Config.DEFAULT_HTTP_TIMEOUT, Config.DEFAULT_HTTP_MAX_RETRY, 1.5f));
		RequestManager.getInstance().getRequestQueue().add(request);
	}
}
