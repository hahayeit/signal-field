package app.signalfield.scanner;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import android.text.*;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.location.*;
import android.webkit.*;
import org.json.*;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    final int BG=Color.rgb(7,18,24), CARD=Color.rgb(15,34,43), GREEN=Color.rgb(100,245,186), TEXT=Color.rgb(231,245,241), MUTED=Color.rgb(151,178,185), AMBER=Color.rgb(245,190,117);
    final Handler handler=new Handler(Looper.getMainLooper());
    final LinkedHashMap<String,JSONObject> wifiItems=new LinkedHashMap<>(),bleItems=new LinkedHashMap<>();
    final LinkedHashMap<String,JSONObject> equipmentItems=new LinkedHashMap<>();
    boolean equipmentOnly=false;
    final ArrayList<JSONObject> history=new ArrayList<>();
    final HashMap<String,Long> lastLogged=new HashMap<>();
    WifiManager wifi; BluetoothLeScanner scanner; LocationManager locations;
    Location location; LinearLayout root,body,list; TextView state,stats; Button action; WebView map;
    int tab=0; String filter="",wifiStatus="Ready · tap Scan Wi-Fi",bleStatus="Ready · tap Start BLE";
    boolean scanning=false,receiverRegistered=false,onlineMap=false,mapReady=false,dirty=false,wifiPending=false,active=false;
    Runnable pendingAction;
    int policeCategory=0; String policeQuery=""; LinearLayout policeRows; TextView policeCount;
    final Runnable wifiTimeout=()->{if(wifiPending){wifiPending=false;wifiStatus="No fresh scan received · Android may be limiting scans";readWifi(false);refresh();}};
    final Runnable stopTimer=()->{stopBle();bleStatus="Scan complete · 60-second session";refresh();};
    final Runnable ticker=new Runnable(){public void run(){if(!active)return;if(dirty||(tab==1&&equipmentOnly&&!equipmentItems.isEmpty())){dirty=false;refresh();}handler.postDelayed(this,1000);}};
    final BroadcastReceiver receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){if(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(i.getAction())){wifiPending=false;handler.removeCallbacks(wifiTimeout);readWifi(i.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED,false));refresh();}}};
    final LocationListener locationListener=new LocationListener(){
        public void onLocationChanged(Location l){location=l;dirty=true;}
        public void onProviderEnabled(String p){}
        public void onProviderDisabled(String p){dirty=true;}
        public void onStatusChanged(String p,int s,Bundle b){}
    };
    final ScanCallback scanCallback=new ScanCallback(){
        public void onScanResult(int t,android.bluetooth.le.ScanResult r){handler.post(()->acceptBle(r));}
        public void onBatchScanResults(List<android.bluetooth.le.ScanResult> results){handler.post(()->{for(android.bluetooth.le.ScanResult r:results)acceptBle(r);});}
        public void onScanFailed(int code){handler.post(()->{stopBle();bleStatus="Bluetooth scan failed (code "+code+") · retry in a moment";refresh();});}
    };

    @Override public void onCreate(Bundle b){
        super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        wifi=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        locations=(LocationManager)getSystemService(LOCATION_SERVICE);
        readHistory();build();
    }
    @Override public void onStart(){super.onStart();active=true;IntentFilter f=new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,f,Context.RECEIVER_EXPORTED);else registerReceiver(receiver,f);receiverRegistered=true;handler.post(ticker);refresh();}
    @Override public void onStop(){super.onStop();active=false;stopBle();if(wifiPending){wifiPending=false;wifiStatus="Scan interrupted · tap Scan Wi-Fi";}handler.removeCallbacks(wifiTimeout);handler.removeCallbacks(ticker);try{locations.removeUpdates(locationListener);}catch(Exception ignored){}if(receiverRegistered){unregisterReceiver(receiver);receiverRegistered=false;}saveHistory();}
    @Override public void onDestroy(){if(map!=null)map.destroy();super.onDestroy();}
    int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    GradientDrawable surface(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    TextView text(String s,int size,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setFontFeatureSettings("tnum");return v;}
    TextView mono(String s,int size,int color){TextView v=text(s,size,color);v.setTypeface(Typeface.MONOSPACE);return v;}
    LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    Button button(String s,Runnable run){Button v=new Button(this);v.setText(s);v.setAllCaps(false);v.setTextSize(13);v.setTextColor(TEXT);v.setBackground(surface(CARD,12));v.setMinHeight(dp(44));v.setMinimumHeight(dp(44));v.setOnClickListener(w->run.run());return v;}
    void gap(LinearLayout p,int h){p.addView(new View(this),new LinearLayout.LayoutParams(1,dp(h)));}
    void build(){
        root=column();root.setBackgroundColor(BG);root.setPadding(dp(20),dp(8),dp(20),dp(8));setContentView(root);
        root.setOnApplyWindowInsetsListener((v,insets)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(dp(20)+i.left,dp(8)+i.top,dp(20)+i.right,dp(8)+i.bottom);}else v.setPadding(dp(20),dp(8)+insets.getSystemWindowInsetTop(),dp(20),dp(8)+insets.getSystemWindowInsetBottom());return insets;});
        root.addView(mono("◉  SIGNAL FIELD",14,GREEN));gap(root,8);
        TextView title=text("Read your surroundings.",25,TEXT);title.setTypeface(null,Typeface.BOLD);root.addView(title);gap(root,4);
        root.addView(text("Wi-Fi · Bluetooth LE · observation map",12,MUTED));gap(root,18);
        LinearLayout tabs=new LinearLayout(this);String[] names={"Wi-Fi","BLE","Map","Log","Police"};for(int i=0;i<names.length;i++){final int t=i;Button b=button(names[i],()->{tab=t;filter="";buildBody();});b.setTextSize(12);b.setPadding(dp(2),0,dp(2),0);b.setMinWidth(0);b.setMinimumWidth(0);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(44),1);p.setMargins(0,0,dp(i<4?4:0),0);tabs.addView(b,p);}root.addView(tabs);gap(root,12);
        body=column();root.addView(body,new LinearLayout.LayoutParams(-1,0,1));buildBody();
    }
    void buildBody(){
        if(map!=null){map.destroy();map=null;mapReady=false;}body.removeAllViews();list=null;state=null;stats=null;action=null;
        if(tab==2){buildMap();return;}if(tab==3){buildLog();return;}if(tab==4){buildPolice();return;}
        TextView label=mono(tab==0?"01 / WI-FI SPECTRUM":"02 / BLUETOOTH LE",12,GREEN);body.addView(label);gap(body,10);
        LinearLayout controls=new LinearLayout(this);
        action=button(tab==0?"Scan Wi-Fi":scanning?"Stop BLE":"Start BLE",()->{if(tab==0)withPermissions(false,()->scanWifi());else if(scanning){stopBle();refresh();}else withPermissions(true,()->startBle());});
        action.setTextColor(BG);action.setBackground(surface(GREEN,12));controls.addView(action,new LinearLayout.LayoutParams(0,dp(46),1));
        Button settings=button("Settings",()->startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(dp(100),dp(46));sp.setMargins(dp(8),0,0,0);controls.addView(settings,sp);body.addView(controls);gap(body,10);
        state=text("",12,MUTED);body.addView(state);gap(body,8);stats=mono("",11,GREEN);body.addView(stats);gap(body,10);
        if(tab==1){
            CheckBox equipmentToggle=new CheckBox(this);equipmentToggle.setText("Possible law-enforcement equipment only");equipmentToggle.setTextSize(13);equipmentToggle.setTextColor(AMBER);equipmentToggle.setChecked(equipmentOnly);body.addView(equipmentToggle);
            equipmentToggle.setOnCheckedChangeListener((b,checked)->{equipmentOnly=checked;refresh();});
            body.addView(button("Matching rules & limits",()->new AlertDialog.Builder(this).setTitle("Possible equipment · unconfirmed").setMessage("Matches advertised manufacturer company ID 0x034D (TASER), or service UUIDs 0xFC81 (Axon), 0xFE6B / 0xFE6C (TASER). Service-data UUIDs are checked too.\n\nThese assignments identify vendors, not vehicle types or operators. Coverage is limited to these identifiers. Names and MAC address prefixes are not used. A match does not decode a camera trigger or prove a device is recording.\n\n"+EquipmentMatcher.LIMITATION+"\n\nRecent means a matching advertisement was received within 30 seconds while scanning. Older matches remain labeled stale; stopping scanning removes the recent status. A new scan clears this session's results.\n\nBluetooth SIG registry checked November 4, 2026. Matching evidence is included in the local log and JSON export.").setPositiveButton("Close",null).setNeutralButton("Identifier source",(d,w)->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(EquipmentMatcher.SOURCE)));}catch(ActivityNotFoundException ex){message("No browser installed");}}).show()));gap(body,8);
        }
        EditText search=new EditText(this);search.setSingleLine(true);search.setTextColor(TEXT);search.setHintTextColor(MUTED);search.setTextSize(13);search.setHint("Filter name or address");search.setPadding(dp(12),0,dp(12),0);search.setBackground(surface(CARD,10));body.addView(search,new LinearLayout.LayoutParams(-1,dp(42)));search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){filter=s.toString().toLowerCase(Locale.ROOT);renderList();}public void afterTextChanged(Editable e){}});gap(body,10);
        ScrollView scroll=new ScrollView(this);list=column();scroll.addView(list);body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView note=text(tab==0?"Android limits Wi-Fi scan frequency. Cached results are labeled.":"Listening for advertisements only. Device identity is unverified.",11,MUTED);note.setPadding(0,dp(8),0,0);body.addView(note);refresh();
    }
    void refresh(){
        if(tab==2){updateMap();return;}if(tab>=3)return;if(state==null)return;
        state.setText(tab==0?wifiStatus:bleStatus);if(action!=null)action.setText(tab==0?"Scan Wi-Fi":scanning?"Stop BLE":"Start BLE");
        if(tab==1&&equipmentOnly){int recent=0;for(JSONObject o:equipmentItems.values())if(EquipmentMatcher.recent(scanning,SystemClock.elapsedRealtimeNanos(),o.optLong("receivedElapsedNanos")))recent++;stats.setText(recent+" RECENT / "+equipmentItems.size()+" SESSION MATCHES · UNCONFIRMED");}
        else stats.setText(String.format(Locale.US,"%02d SIGNALS  /  %s",(tab==0?wifiItems:bleItems).size(),freshLocation()?"GPS FIX ±"+Math.round(location.getAccuracy())+" m":"NO FRESH GPS FIX"));renderList();
    }
    void renderList(){
        if(list==null||tab>1)return;list.removeAllViews();ArrayList<JSONObject> rows=new ArrayList<>((tab==0?wifiItems:equipmentOnly?equipmentItems:bleItems).values());rows.sort((a,b)->Integer.compare(b.optInt("rssi"),a.optInt("rssi")));int count=0;
        for(JSONObject item:rows){if(!(item.optString("name")+" "+item.optString("address")).toLowerCase(Locale.ROOT).contains(filter))continue;count++;
            LinearLayout card=column();card.setPadding(dp(14),dp(12),dp(14),dp(12));card.setBackground(surface(CARD,14));
            LinearLayout top=new LinearLayout(this);TextView name=text(item.optString("name"),16,TEXT);name.setTypeface(null,Typeface.BOLD);name.setMaxLines(2);top.addView(name,new LinearLayout.LayoutParams(0,-2,1));top.addView(mono(item.optInt("rssi")+" dBm",13,GREEN));card.addView(top);gap(card,7);
            card.addView(mono(item.optString("address"),11,MUTED));gap(card,7);
            String age=age(item);String detail=SignalRules.strength(item.optInt("rssi"))+"  ·  "+age;
            if(item.optBoolean("cached"))detail+="  ·  CACHED";card.addView(mono(detail,10,item.optBoolean("cached")?AMBER:GREEN));gap(card,5);
            card.addView(text(tab==0?item.optInt("frequency")+" MHz  ·  "+item.optString("security"):"Advertisement · tap for raw data",11,MUTED));
            if(item.has("equipmentEvidence")){
                gap(card,8);card.addView(text("Possible law-enforcement equipment",14,AMBER));
                JSONArray evidence=item.optJSONArray("equipmentEvidence");if(evidence!=null)for(int i=0;i<evidence.length();i++)card.addView(text(evidence.optString(i),11,MUTED));
                boolean recent=EquipmentMatcher.recent(scanning,SystemClock.elapsedRealtimeNanos(),item.optLong("receivedElapsedNanos"));
                card.addView(mono(!scanning?"SCAN STOPPED · RETAINED MATCH":recent?"RECENT ADVERTISEMENT · UNCONFIRMED":"STALE MATCH · NOT CURRENT PRESENCE",10,recent?GREEN:AMBER));
                card.addView(text("Last matching broadcast: "+new java.text.SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(new Date(item.optLong("time")))+" · RSSI is not distance",11,MUTED));
            }
            card.setOnClickListener(v->showDetail(item));list.addView(card,new LinearLayout.LayoutParams(-1,-2));gap(list,8);
        }
        if(count==0){gap(list,24);boolean equipment=tab==1&&equipmentOnly;list.addView(text(equipment?"No matching equipment broadcasts.":filter.isEmpty()?"The field is quiet.":"No matching signals.",21,TEXT));gap(list,8);list.addView(text(equipment?"Start BLE to listen for supported identifiers. No matches does not mean no police nearby. A text filter may also hide results.":filter.isEmpty()?"Start a scan to see nearby broadcasts. No sample devices or simulated detections are shown.":"Try another name or address.",14,MUTED));}
    }
    String age(JSONObject item){long seconds=Math.max(0,(System.currentTimeMillis()-item.optLong("time"))/1000);return seconds<60?seconds+"s ago":seconds/60+"m ago";}
    void showDetail(JSONObject item){try{TextView t=mono(item.toString(2),12,TEXT);t.setTextIsSelectable(true);t.setPadding(dp(18),dp(12),dp(18),dp(12));ScrollView s=new ScrollView(this);s.addView(t);new AlertDialog.Builder(this).setTitle("Broadcast details · unverified identity").setView(s).setPositiveButton("Close",null).show();}catch(Exception e){message(e.getMessage());}}
    boolean granted(String p){return checkSelfPermission(p)==PackageManager.PERMISSION_GRANTED;}
    void withPermissions(boolean ble,Runnable next){ArrayList<String> needed=new ArrayList<>();if(!granted(Manifest.permission.ACCESS_FINE_LOCATION)){needed.add(Manifest.permission.ACCESS_FINE_LOCATION);needed.add(Manifest.permission.ACCESS_COARSE_LOCATION);}if(ble&&Build.VERSION.SDK_INT>=31){if(!granted(Manifest.permission.BLUETOOTH_SCAN))needed.add(Manifest.permission.BLUETOOTH_SCAN);if(!granted(Manifest.permission.BLUETOOTH_CONNECT))needed.add(Manifest.permission.BLUETOOTH_CONNECT);}if(needed.isEmpty()){next.run();return;}pendingAction=next;new AlertDialog.Builder(this).setTitle("Allow nearby scanning").setMessage("Android requires precise location for Wi-Fi scanning and map observations. Bluetooth also needs Nearby devices permission on Android 12+. Scans stay on this phone unless you export them. Choose Precise and While using the app.").setPositiveButton("Continue",(d,w)->requestPermissions(needed.toArray(new String[0]),100)).setNegativeButton("Cancel",(d,w)->pendingAction=null).show();}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request!=100)return;boolean ok=results.length>0;for(int r:results)ok&=r==PackageManager.PERMISSION_GRANTED;Runnable next=pendingAction;pendingAction=null;if(ok&&next!=null)next.run();else new AlertDialog.Builder(this).setTitle("Permission needed").setMessage("Scanning requires the requested permissions, including precise location. You can enable them in Android app settings.").setPositiveButton("App settings",(d,w)->startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())))).setNegativeButton("Close",null).show();}
    boolean locationEnabled(){return Build.VERSION.SDK_INT>=28?locations.isLocationEnabled():locations.isProviderEnabled(LocationManager.GPS_PROVIDER)||locations.isProviderEnabled(LocationManager.NETWORK_PROVIDER);}
    void requestLocation(){if(!granted(Manifest.permission.ACCESS_FINE_LOCATION))return;try{for(String provider:new String[]{LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER})if(locations.isProviderEnabled(provider))locations.requestLocationUpdates(provider,5000,3,locationListener);}catch(Exception e){message("Location unavailable: "+e.getMessage());}}
    boolean freshLocation(){return location!=null&&SignalRules.freshFix(SystemClock.elapsedRealtimeNanos(),location.getElapsedRealtimeNanos());}
    JSONObject base(String kind,String name,String address,int rssi){JSONObject o=new JSONObject();put(o,"kind",kind);put(o,"name",name==null||name.isEmpty()?"[unnamed]":name);put(o,"address",address);put(o,"rssi",rssi);put(o,"time",System.currentTimeMillis());put(o,"identity","Unverified broadcast; not proof of device type");return o;}
    void attachLocation(JSONObject o){if(freshLocation()){put(o,"latitude",location.getLatitude());put(o,"longitude",location.getLongitude());put(o,"accuracyMeters",location.getAccuracy());put(o,"locationMeaning","Phone observation location, NOT transmitter location");}}
    void put(JSONObject o,String key,Object value){try{o.put(key,value);}catch(JSONException ignored){}}
    void scanWifi(){
        if(wifi==null){wifiStatus="Wi-Fi hardware unavailable";refresh();return;}if(!locationEnabled()){wifiStatus="Turn on Android Location services to scan";refresh();startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));return;}
        if(!wifi.isWifiEnabled()){wifiStatus="Turn on Wi-Fi in Android settings";refresh();startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));return;}
        requestLocation();try{boolean started=wifi.startScan();wifiPending=started;handler.removeCallbacks(wifiTimeout);if(started){wifiStatus="Scan requested · waiting for fresh results";handler.postDelayed(wifiTimeout,15000);}else{readWifi(false);wifiStatus="Scan limited by Android · showing cached results";}refresh();}catch(SecurityException e){wifiStatus="Permission denied · enable precise location";refresh();}
    }
    void readWifi(boolean updated){
        try{if(wifi==null)return;List<android.net.wifi.ScanResult> results=wifi.getScanResults();wifiItems.clear();for(android.net.wifi.ScanResult r:results){JSONObject o=base("Wi-Fi",r.SSID,r.BSSID,r.level);boolean fresh=updated&&SignalRules.freshWifi(SystemClock.elapsedRealtimeNanos()/1000,r.timestamp);put(o,"cached",!fresh);put(o,"frequency",r.frequency);put(o,"security",r.capabilities);long elapsed=SystemClock.elapsedRealtimeNanos()/1000-r.timestamp;if(elapsed>=0)put(o,"time",System.currentTimeMillis()-elapsed/1000);if(fresh){attachLocation(o);record(o);}wifiItems.put(r.BSSID,o);}wifiStatus=updated?"Scan received · "+results.size()+" networks":"Cached results · no new scan confirmed";}catch(SecurityException e){wifiStatus="Scan unavailable · check location permission";}
    }
    void startBle(){
        try{BluetoothManager manager=(BluetoothManager)getSystemService(BLUETOOTH_SERVICE);BluetoothAdapter adapter=manager==null?null:manager.getAdapter();if(adapter==null){bleStatus="Bluetooth hardware unavailable";refresh();return;}if(!adapter.isEnabled()){bleStatus="Turn on Bluetooth to scan";refresh();startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));return;}if(!locationEnabled()){bleStatus="Turn on Android Location services to scan";refresh();startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));return;}scanner=adapter.getBluetoothLeScanner();if(scanner==null){bleStatus="BLE scanner unavailable";refresh();return;}
            requestLocation();bleItems.clear();equipmentItems.clear();scanning=true;scanner.startScan(null,new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),scanCallback);bleStatus="Listening · stops automatically after 60 seconds";handler.removeCallbacks(stopTimer);handler.postDelayed(stopTimer,60000);refresh();
        }catch(Exception e){stopBle();bleStatus="Cannot scan: "+e.getMessage();refresh();}
    }
    void stopBle(){handler.removeCallbacks(stopTimer);if(scanning){try{if(scanner!=null)scanner.stopScan(scanCallback);}catch(Exception ignored){}scanning=false;bleStatus="Stopped · results retained";}}
    void acceptBle(android.bluetooth.le.ScanResult r){if(!scanning)return;try{ScanRecord record=r.getScanRecord();JSONObject o=base("BLE",record==null?null:record.getDeviceName(),r.getDevice().getAddress(),r.getRssi());put(o,"advertisementHex",record==null?"":SignalRules.hex(record.getBytes()));put(o,"serviceUuids",record==null||record.getServiceUuids()==null?"":record.getServiceUuids().toString());if(record!=null){JSONArray manufacturers=new JSONArray();android.util.SparseArray<byte[]> data=record.getManufacturerSpecificData();for(int i=0;i<data.size();i++){JSONObject m=new JSONObject();put(m,"companyId",data.keyAt(i));put(m,"dataHex",SignalRules.hex(data.valueAt(i)));manufacturers.put(m);}put(o,"manufacturerData",manufacturers);}annotateEquipment(o,record,r.getTimestampNanos());attachLocation(o);String address=o.optString("address");if(!bleItems.containsKey(address)&&bleItems.size()>=500)bleItems.remove(bleItems.keySet().iterator().next());bleItems.put(address,o);record(o);dirty=true;}catch(SecurityException e){stopBle();bleStatus="Bluetooth permission revoked";dirty=true;}}
    void annotateEquipment(JSONObject o,ScanRecord record,long receivedNanos){
        put(o,"receivedElapsedNanos",receivedNanos);
        long elapsed=SystemClock.elapsedRealtimeNanos()-receivedNanos;
        if(receivedNanos>0&&elapsed>=0)put(o,"time",System.currentTimeMillis()-elapsed/1000000L);
        if(record==null)return;
        android.util.SparseArray<byte[]> manufacturer=record.getManufacturerSpecificData();
        int[] ids=new int[manufacturer.size()];for(int i=0;i<ids.length;i++)ids[i]=manufacturer.keyAt(i);
        ArrayList<String> services=new ArrayList<>();
        if(record.getServiceUuids()!=null)for(ParcelUuid uuid:record.getServiceUuids())services.add(uuid.toString());
        JSONArray serviceData=new JSONArray();
        if(record.getServiceData()!=null)for(Map.Entry<ParcelUuid,byte[]> entry:record.getServiceData().entrySet()){
            services.add(entry.getKey().toString());JSONObject data=new JSONObject();put(data,"uuid",entry.getKey().toString());put(data,"dataHex",SignalRules.hex(entry.getValue()));serviceData.put(data);
        }
        put(o,"serviceData",serviceData);
        List<String> reasons=EquipmentMatcher.match(ids,services);if(reasons.isEmpty())return;
        put(o,"equipmentEvidence",new JSONArray(reasons));put(o,"equipmentAssessment","Possible law-enforcement equipment; unconfirmed");
        put(o,"equipmentRuleVersion",EquipmentMatcher.RULE_VERSION);put(o,"equipmentSource",EquipmentMatcher.SOURCE);put(o,"equipmentLimitations",EquipmentMatcher.LIMITATION);
        String address=o.optString("address");if(!equipmentItems.containsKey(address)&&equipmentItems.size()>=500)equipmentItems.remove(equipmentItems.keySet().iterator().next());
        equipmentItems.put(address,o);
    }
    void record(JSONObject o){String key=o.optString("kind")+o.optString("address")+"|"+o.optString("equipmentEvidence");long time=o.optLong("time");Long previous=lastLogged.get(key);if(previous!=null&&time-previous<10000)return;lastLogged.put(key,time);if(lastLogged.size()>4000)lastLogged.clear();history.add(o);while(history.size()>3000)history.remove(0);}

    void buildPolice(){
        body.addView(button("Scan possible equipment via BLE",()->{tab=1;filter="";equipmentOnly=true;buildBody();}));gap(body,8);
        body.addView(mono("05 / LOCAL POLICE REFERENCE",12,GREEN));gap(body,6);
        body.addView(text("Monroe / Ouachita Parish, LA",18,TEXT));gap(body,8);
        body.addView(text("Offline directory only. This phone cannot tune these radio channels. Filtering does not detect nearby officers or identify Wi-Fi/BLE devices.",12,AMBER));gap(body,8);
        Spinner categories=new Spinner(this);
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,PoliceDirectory.FILTERS){
            @Override public View getView(int position,View convert,android.view.ViewGroup parent){TextView v=(TextView)super.getView(position,convert,parent);v.setTextColor(TEXT);v.setTextSize(14);return v;}
        };adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);categories.setAdapter(adapter);categories.setSelection(policeCategory);body.addView(categories,new LinearLayout.LayoutParams(-1,dp(44)));
        EditText search=new EditText(this);search.setSingleLine(true);search.setTextColor(TEXT);search.setHintTextColor(MUTED);search.setTextSize(13);search.setHint("Agency, MHz frequency or talkgroup ID");search.setPadding(dp(12),0,dp(12),0);search.setBackground(surface(CARD,10));search.setText(policeQuery);body.addView(search,new LinearLayout.LayoutParams(-1,dp(42)));gap(body,8);
        policeCount=mono("",11,GREEN);body.addView(policeCount);gap(body,6);
        ScrollView scroll=new ScrollView(this);policeRows=column();scroll.addView(policeRows);body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        categories.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> parent,View view,int position,long id){policeCategory=position;renderPolice();}public void onNothingSelected(AdapterView<?> parent){}});
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){policeQuery=s.toString();renderPolice();}public void afterTextChanged(Editable e){}});
        gap(body,6);body.addView(button("How to use this directory",()->new AlertDialog.Builder(this).setTitle("Police radio reference").setMessage("Police / sheriff shows the listed local dispatch talkgroups, sheriff backup and interop channels. This is a starter directory, not a complete inventory.\n\nLWIN is a shared trunked system. Filter by agency talkgroup ID on a compatible P25 scanner/receiver; a site frequency alone does not isolate police. This APK has no external receiver integration or radio audio decoding. It cannot decrypt encrypted traffic.\n\nShared LWIN site, Corrections and Other / unverified are separate filters. The commercial 400 MHz listing is not established as police.\n\nEntries are based on your screenshots and linked RadioReference listings checked November 4, 2026. Tap an entry for its source/date or to copy its identifier. No live activity is asserted.").setPositiveButton("Close",null).show()));renderPolice();
    }
    void renderPolice(){
        if(tab!=4||policeRows==null)return;policeRows.removeAllViews();int count=0;
        for(PoliceDirectory.Entry e:PoliceDirectory.entries())if(e.matches(policeCategory,policeQuery)){
            count++;LinearLayout card=column();card.setPadding(dp(14),dp(12),dp(14),dp(12));card.setBackground(surface(CARD,14));
            card.addView(mono(e.displayValue(),18,GREEN));gap(card,4);card.addView(text(e.name,15,TEXT));gap(card,5);card.addView(mono(e.kind.toUpperCase(Locale.ROOT),10,AMBER));gap(card,5);card.addView(text(e.details,12,MUTED));
            card.setOnClickListener(v->{AlertDialog.Builder dialog=new AlertDialog.Builder(this).setTitle(e.name).setMessage(e.displayValue()+"\n\n"+e.details+"\n\n"+e.provenance+"\n\nReference only; not a live detection.").setNegativeButton("Close",null).setPositiveButton("Copy ID / frequency",(d,w)->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText(e.kind,e.value));message("Copied "+e.displayValue());});if(!e.source.isEmpty())dialog.setNeutralButton("Source",(d,w)->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(e.source)));}catch(ActivityNotFoundException ex){message("No browser installed");}});dialog.show();});
            policeRows.addView(card);gap(policeRows,8);
        }
        policeCount.setText(count+" LISTED ENTRIES · REFERENCE ONLY");
        if(count==0){policeRows.addView(text("No matching entries.",18,TEXT));gap(policeRows,6);policeRows.addView(text("Try another term or select All entries. Shared site channels are excluded from Police / sheriff.",13,MUTED));}
    }
    void buildMap(){
        body.addView(mono("03 / OBSERVATION MAP",12,GREEN));gap(body,8);body.addView(text("Pins mark where your phone heard a signal. They do not locate the transmitter.",12,MUTED));gap(body,10);
        Button locate=button("Enable / refresh my location",()->withPermissions(false,()->{requestLocation();updateMap();}));body.addView(locate);gap(body,10);
        if(!onlineMap){gap(body,24);body.addView(text("Put your readings in context.",22,TEXT));gap(body,10);body.addView(text("Satellite imagery comes from Esri; street tiles from OpenStreetMap. Loading the map shares your IP address and the viewed map area with the tile provider. Scan names and addresses are not uploaded. Scanning works without loading the map.",14,MUTED));gap(body,16);body.addView(button("Load online map",()->{onlineMap=true;buildBody();}));return;}
        map=new WebView(this);map.setBackgroundColor(CARD);map.getSettings().setJavaScriptEnabled(true);map.getSettings().setAllowFileAccess(false);map.getSettings().setAllowContentAccess(false);map.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        map.setWebViewClient(new WebViewClient(){
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest req){Uri u=req.getUrl();if("appassets.androidplatform.net".equals(u.getHost())){try{String path=u.getPath().substring(1);if(path.contains(".."))throw new IOException();String mime=path.endsWith(".html")?"text/html":path.endsWith(".css")?"text/css":path.endsWith(".png")?"image/png":"application/javascript";return new WebResourceResponse(mime,"UTF-8",getAssets().open(path));}catch(Exception e){return new WebResourceResponse("text/plain","UTF-8",404,"Not found",null,new ByteArrayInputStream(new byte[0]));}}String host=u.getHost();if("https".equals(u.getScheme())&&("tile.openstreetmap.org".equals(host)||"server.arcgisonline.com".equals(host)))return null;return new WebResourceResponse("text/plain","UTF-8",403,"Blocked",null,new ByteArrayInputStream(new byte[0]));}
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest req){Uri u=req.getUrl();if("appassets.androidplatform.net".equals(u.getHost()))return false;if("https".equals(u.getScheme()))try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception ignored){}return true;}
            @Override public void onPageFinished(WebView v,String url){mapReady=true;updateMap();}
        });body.addView(map,new LinearLayout.LayoutParams(-1,0,1));map.loadUrl("https://appassets.androidplatform.net/map.html");
    }
    void updateMap(){if(map==null||!mapReady)return;JSONObject data=new JSONObject();JSONArray points=new JSONArray();for(int i=Math.max(0,history.size()-500);i<history.size();i++)if(history.get(i).has("latitude"))points.put(history.get(i));put(data,"observations",points);if(freshLocation()){JSONObject p=new JSONObject();put(p,"latitude",location.getLatitude());put(p,"longitude",location.getLongitude());put(p,"accuracy",location.getAccuracy());put(data,"location",p);}map.evaluateJavascript("updateData("+data.toString()+")",null);}
    void buildLog(){
        body.addView(mono("04 / FIELD LOG",12,GREEN));gap(body,16);body.addView(text(history.size()+" observations",27,TEXT));gap(body,8);body.addView(text("Stored privately on this phone. Up to 3,000 recent observations; repeated devices are logged at most once per 10 seconds. Cached Wi-Fi results are excluded.",13,MUTED));gap(body,14);
        body.addView(button("Export JSON…",()->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/json");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,"signal-field-"+new java.text.SimpleDateFormat("yyyyMMdd-HHmmss",Locale.US).format(new Date())+".json");startActivityForResult(i,200);}));gap(body,8);
        body.addView(button("Clear field log",()->new AlertDialog.Builder(this).setTitle("Clear saved observations?").setMessage("This deletes the local field log and map pins. Exported files are unaffected.").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,w)->{history.clear();lastLogged.clear();saveHistory();buildBody();}).show()));gap(body,16);
        ScrollView scroll=new ScrollView(this);LinearLayout rows=column();scroll.addView(rows);body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(history.isEmpty())rows.addView(text("Your next scan starts the story.",17,MUTED));
        for(int i=history.size()-1;i>=Math.max(0,history.size()-50);i--){JSONObject o=history.get(i);TextView row=text(o.optString("kind")+"  ·  "+o.optString("name")+"\n"+o.optInt("rssi")+" dBm  ·  "+age(o)+(o.has("latitude")?"  ·  GPS tagged":"  ·  no location"),13,TEXT);row.setPadding(0,dp(8),0,dp(8));row.setOnClickListener(v->showDetail(o));rows.addView(row);}
        gap(body,8);body.addView(button("About & limitations",()->new AlertDialog.Builder(this).setTitle("Signal Field 1.2").setMessage("A standalone scanner inspired by the supplied video. No account, analytics, background tracking, or simulated detections.\n\nWi-Fi and BLE expose broadcasts, not a reliable camera identity. This app does not certify Flock cameras, estimate exact range, triangulate devices, or access camera feeds. Signal strength is affected by obstacles, antenna power and orientation.\n\nScanning stops when the app leaves the foreground. Map imagery needs internet. Exports include device addresses and any recorded phone locations.\n\nMap renderer: Leaflet 1.9.4 (BSD-2-Clause). Map imagery: Esri / OpenStreetMap, with attribution on the map.").setPositiveButton("Close",null).show()));
    }
    JSONObject exportData(){JSONObject o=new JSONObject();put(o,"app","Signal Field");put(o,"version",1);put(o,"exportedAt",System.currentTimeMillis());put(o,"locationMeaning","Phone observation positions, not transmitter locations");put(o,"observations",new JSONArray(history));return o;}
    @Override public void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==200&&result==RESULT_OK&&data!=null&&data.getData()!=null){try(OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out==null)throw new IOException("Cannot open destination");out.write(exportData().toString(2).getBytes(StandardCharsets.UTF_8));message("Field log exported");}catch(Exception e){message("Export failed: "+e.getMessage());}}}
    void saveHistory(){android.util.AtomicFile f=new android.util.AtomicFile(new File(getFilesDir(),"observations.json"));FileOutputStream out=null;try{out=f.startWrite();out.write(exportData().toString().getBytes(StandardCharsets.UTF_8));f.finishWrite(out);}catch(Exception e){if(out!=null)f.failWrite(out);message("Could not save field log");}}
    void readHistory(){try{android.util.AtomicFile f=new android.util.AtomicFile(new File(getFilesDir(),"observations.json"));JSONObject data=new JSONObject(new String(f.readFully(),StandardCharsets.UTF_8));JSONArray a=data.optJSONArray("observations");if(a!=null)for(int i=Math.max(0,a.length()-3000);i<a.length();i++)history.add(a.getJSONObject(i));}catch(FileNotFoundException ignored){}catch(Exception e){message("Saved log could not be read");}}
    void message(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
