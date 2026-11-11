package org.folio.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Standalone, permission-light Folio Home. No privileged SystemUI controls are simulated. */
public class HomeActivity extends Activity {
    private static final int PICK_WIDGET = 71;
    private static final int BIND_WIDGET = 72;
    private static final int CONFIG_WIDGET = 73;
    private static final int HOST_ID = 20831;
    private final int CREAM = Color.rgb(247,245,241), INK = Color.rgb(48,48,56);
    private final int SAGE = Color.rgb(96,132,113), MUTED = Color.rgb(110,108,110);
    private SharedPreferences prefs;
    private PackageManager pm;
    private AppWidgetManager widgetManager;
    private AppWidgetHost widgetHost;
    private int pendingWidgetId = -1;
    private FrameLayout root;
    private LinearLayout page;
    private boolean dark;
    private final List<AppItem> installed = new ArrayList<>();
    private final Map<String, AppItem> byId = new HashMap<>();
    private final List<String> homeItems = new ArrayList<>();
    private final Map<String, List<String>> folders = new HashMap<>();
    private final Map<String, String> folderNames = new HashMap<>();
    private String view = "home";
    private float downX, downY;
    private boolean gestureTracking;

    static class AppItem {
        String id, label;
        Drawable icon;
        Intent launch;
        AppItem(String id, String label, Drawable icon, Intent launch) {
            this.id=id; this.label=label; this.icon=icon; this.launch=launch;
        }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=getSharedPreferences("folio_home",MODE_PRIVATE);
        pm=getPackageManager(); dark=prefs.getBoolean("dark",false);
        widgetManager=AppWidgetManager.getInstance(this);
        widgetHost=new AppWidgetHost(this,HOST_ID);
        if(state!=null)pendingWidgetId=state.getInt("pending_widget_id",-1);
        loadApps(); loadHome(); showHome();
    }
    @Override protected void onResume(){super.onResume(); try {widgetHost.startListening();}catch(Exception ignored){} loadApps(); if("home".equals(view))showHome();}
    @Override protected void onSaveInstanceState(Bundle out){out.putInt("pending_widget_id",pendingWidgetId);super.onSaveInstanceState(out);}
    @Override protected void onPause(){super.onPause(); try {widgetHost.stopListening();}catch(Exception ignored){}}
    @Override public void onBackPressed(){if(!"home".equals(view))showHome();else super.onBackPressed();}

    private int dp(float value){return (int)(value*getResources().getDisplayMetrics().density+0.5f);}
    private int background(){return dark ? Color.rgb(30,31,34) : CREAM;}
    private int surface(){return dark ? Color.rgb(43,44,49) : Color.WHITE;}
    private int ink(){return dark ? Color.rgb(242,238,232) : INK;}
    private int secondary(){return dark ? Color.rgb(183,181,179) : MUTED;}
    private int border(){return dark ? Color.rgb(78,79,83) : Color.rgb(230,226,222);}
    private android.graphics.drawable.GradientDrawable shape(int color,int radius, int stroke){
        android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius)); if(stroke!=0)d.setStroke(dp(1),stroke);return d;
    }
    private TextView text(String s,int sp,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(TypedValue.COMPLEX_UNIT_SP,sp);t.setGravity(Gravity.CENTER_VERTICAL); if(bold)t.setTypeface(null,1);return t;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));}
    private void addSpace(LinearLayout l,int n){View v=new View(this);l.addView(v,lp(1,n));}
    private TextView button(String label,Runnable click){TextView b=text(label,14,ink(),true);b.setGravity(Gravity.CENTER);b.setMinHeight(dp(48));b.setPadding(dp(14),dp(8),dp(14),dp(8));b.setBackground(shape(surface(),14,border()));b.setOnClickListener(v->click.run());return b;}

    private void base(String current){
        view=current;
        root=new FrameLayout(this);root.setBackgroundColor(background());
        page=column();page.setPadding(dp(18),dp(10),dp(18),dp(12));
        root.addView(page,new FrameLayout.LayoutParams(-1,-1));
        setContentView(root);
        getWindow().setStatusBarColor(background());getWindow().setNavigationBarColor(background());
        getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }
    private void loadApps(){
        installed.clear();byId.clear();
        Intent q=new Intent(Intent.ACTION_MAIN);q.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> results=pm.queryIntentActivities(q,0);
        for(ResolveInfo ri:results){
            if(ri.activityInfo==null)continue;
            ComponentName component=new ComponentName(ri.activityInfo.packageName,ri.activityInfo.name);
            if(component.getPackageName().equals(getPackageName()))continue;
            Intent go=new Intent(Intent.ACTION_MAIN);go.addCategory(Intent.CATEGORY_LAUNCHER);go.setComponent(component);go.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            AppItem item=new AppItem(component.flattenToString(),ri.loadLabel(pm).toString(),ri.loadIcon(pm),go);
            installed.add(item);byId.put(item.id,item);
        }
        Collections.sort(installed,Comparator.comparing(a->a.label.toLowerCase(Locale.ITALIAN)));
    }
    private void loadHome(){
        homeItems.clear();folders.clear();folderNames.clear();
        String saved=prefs.getString("home_json","");
        if(!saved.isEmpty())try {
            JSONObject state=new JSONObject(saved);JSONArray pinned=state.optJSONArray("items");
            if(pinned!=null)for(int i=0;i<pinned.length();i++)homeItems.add(pinned.optString(i));
            JSONObject f=state.optJSONObject("folders");if(f!=null){
                java.util.Iterator<String> keys=f.keys();while(keys.hasNext()){
                    String k=keys.next();JSONObject data=f.optJSONObject(k);if(data==null)continue;
                    folderNames.put(k,data.optString("name","Cartella"));
                    JSONArray arr=data.optJSONArray("apps");List<String> ids=new ArrayList<>();
                    if(arr!=null)for(int i=0;i<arr.length();i++)ids.add(arr.optString(i));folders.put(k,ids);
                }
            }
        }catch(Exception ignored){}
        if(saved.isEmpty()){
            int n=Math.min(installed.size(),8);for(int i=0;i<n;i++)homeItems.add(installed.get(i).id);
            saveHome();
        }
    }
    private void saveHome(){
        try{JSONObject state=new JSONObject();state.put("items",new JSONArray(homeItems));JSONObject f=new JSONObject();
            for(String id:folders.keySet()){JSONObject obj=new JSONObject();obj.put("name",folderNames.get(id));obj.put("apps",new JSONArray(folders.get(id)));f.put(id,obj);}state.put("folders",f);
            prefs.edit().putString("home_json",state.toString()).apply();
        }catch(JSONException ignored){}
    }
    private ImageView appImage(AppItem a,int dimension){ImageView i=new ImageView(this);i.setImageBitmap(maskedIcon(a,dimension));i.setScaleType(ImageView.ScaleType.FIT_CENTER);return i;}
    private Bitmap maskedIcon(AppItem a,int sizeDp){
        int px=dp(sizeDp), mode=prefs.getInt("icon_mode",1);
        Bitmap bm=Bitmap.createBitmap(px,px,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(bm);if(a.icon==null)return bm;
        Drawable d=a.icon.getConstantState()!=null ? a.icon.getConstantState().newDrawable().mutate() : a.icon;
        if(mode==0){d.setBounds(0,0,px,px);d.draw(c);return bm;}
        if(mode==2 && android.os.Build.VERSION.SDK_INT>=33 && d instanceof AdaptiveIconDrawable){
            Drawable mono=((AdaptiveIconDrawable)d).getMonochrome();
            if(mono!=null){c.drawRoundRect(new RectF(0,0,px,px),dp(12),dp(12),paint(dark?0xff4f665b:0xffdce8df));mono.setTint(SAGE);mono.setBounds(dp(7),dp(7),px-dp(7),px-dp(7));mono.draw(c);return bm;}
        }
        Path path=new Path();path.addRoundRect(new RectF(0,0,px,px),dp(12),dp(12),Path.Direction.CW);c.save();c.clipPath(path);
        c.drawColor(dark?0xff35363a:0xffeee9e1);
        int inset=d instanceof AdaptiveIconDrawable?0:dp(5);
        d.setBounds(inset,inset,px-inset,px-inset);d.draw(c);c.restore();return bm;
    }
    private Paint paint(int color){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);return p;}
    private void showHome(){
        base("home");
        LinearLayout header=row();
        LinearLayout title=column();
        String today=new SimpleDateFormat("EEEE d MMMM",Locale.ITALIAN).format(new Date());
        title.addView(text(today,13,secondary(),false));title.addView(text("Folio",27,ink(),true));
        header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        TextView settings=button("⚙",this::showSettings);settings.setContentDescription("Impostazioni Folio Home");header.addView(settings,lp(50,50));
        page.addView(header);addSpace(page,14);
        TextView search=button("⌕   Cerca applicazioni",()->showDrawer(true));search.setGravity(Gravity.CENTER_VERTICAL);page.addView(search,lp(-1,52));addSpace(page,14);
        LinearLayout nav=row();
        TextView news=button("Notizie",this::launchNews);nav.addView(news,new LinearLayout.LayoutParams(0,dp(50),1));
        TextView browser=button("Browser",this::launchBrowser);LinearLayout.LayoutParams nlp=new LinearLayout.LayoutParams(0,dp(50),1);nlp.leftMargin=dp(8);nav.addView(browser,nlp);
        TextView files=button("File",this::launchFiles);nlp=new LinearLayout.LayoutParams(0,dp(50),1);nlp.leftMargin=dp(8);nav.addView(files,nlp);
        page.addView(nav);addSpace(page,12);
        TextView widgetTitle=text("Widget Android",14,ink(),true);
        LinearLayout widgetHeading=row();widgetHeading.addView(widgetTitle,new LinearLayout.LayoutParams(0,-2,1));
        widgetHeading.addView(button("+ Aggiungi",this::pickWidget),lp(-2,48));page.addView(widgetHeading);addSpace(page,8);
        LinearLayout widgetContainer=column();widgetContainer.setGravity(Gravity.CENTER);widgetContainer.setBackground(shape(surface(),18,border()));
        widgetContainer.setMinimumHeight(dp(110));page.addView(widgetContainer,lp(-1,-2));inflateWidget(widgetContainer);
        addSpace(page,16);
        TextView appsTitle=text("Le tue app",14,ink(),true);page.addView(appsTitle);addSpace(page,8);
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setVerticalScrollBarEnabled(false);
        GridLayout grid=new GridLayout(this);grid.setColumnCount(4);grid.setPadding(0,dp(6),0,dp(12));
        for(String id:new ArrayList<>(homeItems)){
            if(id.startsWith("folder:")){addTile(grid,id,folderNames.getOrDefault(id,"Cartella"),null,()->openFolder(id),()->folderMenu(id));}
            else{AppItem item=byId.get(id);if(item!=null)addTile(grid,id,item.label,item,()->open(item),()->appMenu(item,true));}
        }
        sc.addView(grid);page.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        addSpace(page,8);
        TextView all=button("Tutte le applicazioni  ↑",()->showDrawer(false));page.addView(all,lp(-1,54));
        // Native status bar remains available for Android notifications and Quick Settings.

    }
    @Override public boolean dispatchTouchEvent(MotionEvent e){
        if("home".equals(view)){
            if(e.getActionMasked()==MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();gestureTracking=true;}
            else if(e.getActionMasked()==MotionEvent.ACTION_UP && gestureTracking){
                gestureTracking=false;float dx=e.getX()-downX,dy=e.getY()-downY;
                if(Math.abs(dx)>dp(110)&&Math.abs(dx)>Math.abs(dy)*1.6f){
                    if(dx>0)launchNews();else showDrawer(false);
                    return true;
                }
                if(dy< -dp(130)&&Math.abs(dy)>Math.abs(dx)*1.6f){showDrawer(false);return true;}
            }else if(e.getActionMasked()==MotionEvent.ACTION_CANCEL){gestureTracking=false;}
        }
        return super.dispatchTouchEvent(e);
    }
    private void addTile(GridLayout grid,String id,String label,AppItem app,Runnable click,Runnable hold){
        int w=(getResources().getDisplayMetrics().widthPixels-dp(36))/4;
        LinearLayout tile=column();tile.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);tile.setPadding(dp(3),dp(6),dp(3),dp(6));tile.setMinimumHeight(dp(102));
        if(app!=null){ImageView im=appImage(app,57);tile.addView(im,lp(57,57));}
        else{
            TextView folder=text("▦",36,SAGE,true);folder.setGravity(Gravity.CENTER);folder.setBackground(shape(surface(),13,border()));tile.addView(folder,lp(57,57));
        }
        addSpace(tile,5);
        TextView txt=text(label,11,ink(),false);txt.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);txt.setMaxLines(2);txt.setEllipsize(android.text.TextUtils.TruncateAt.END);tile.addView(txt,lp(-1,34));
        tile.setContentDescription(label);tile.setOnClickListener(v->click.run());tile.setOnLongClickListener(v->{hold.run();return true;});
        GridLayout.LayoutParams params=new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED),GridLayout.spec(GridLayout.UNDEFINED));
        params.width=w;grid.addView(tile,params);
    }
    private void open(AppItem a){try{startActivity(a.launch);}catch(Exception ex){Toast.makeText(this,"Impossibile aprire "+a.label,Toast.LENGTH_SHORT).show();loadApps();}}
    private void showDrawer(boolean focus){
        loadApps();base("drawer");
        LinearLayout top=row();top.addView(button("‹ Home",this::showHome));TextView heading=text("Tutte le applicazioni",20,ink(),true);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(0,-2,1);hp.leftMargin=dp(12);top.addView(heading,hp);page.addView(top);addSpace(page,12);
        EditText input=new EditText(this);input.setSingleLine(true);input.setTextSize(16);input.setTextColor(ink());input.setHintTextColor(secondary());input.setHint("Cerca un'app");
        input.setPadding(dp(14),dp(10),dp(14),dp(10));input.setBackground(shape(surface(),15,border()));page.addView(input,lp(-1,52));addSpace(page,10);
        ScrollView scroll=new ScrollView(this);LinearLayout items=column();scroll.addView(items);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        Runnable redraw=()->{items.removeAllViews();String q=input.getText().toString().toLowerCase(Locale.ITALIAN).trim();
            for(AppItem a:installed){if(!a.label.toLowerCase(Locale.ITALIAN).contains(q))continue;
                LinearLayout r=row();r.setPadding(dp(8),dp(4),dp(8),dp(4));r.setMinimumHeight(dp(64));r.addView(appImage(a,43),lp(43,43));
                TextView lab=text(a.label,15,ink(),false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.leftMargin=dp(13);r.addView(lab,p);
                r.setOnClickListener(v->open(a));r.setOnLongClickListener(v->{appMenu(a,false);return true;});items.addView(r);
            }
            if(items.getChildCount()==0){TextView empty=text("Nessun risultato",14,secondary(),false);empty.setPadding(dp(12),dp(22),0,0);items.addView(empty);}
        };
        input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){redraw.run();}public void afterTextChanged(Editable e){}});
        redraw.run();if(focus){input.requestFocus();input.postDelayed(()->((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(input,InputMethodManager.SHOW_IMPLICIT),150);}
    }
    private void appMenu(AppItem app,boolean onHome){
        List<String> actions=new ArrayList<>();actions.add(onHome?"Rimuovi dalla Home":"Aggiungi alla Home");actions.add("Aggiungi a una cartella");actions.add("Crea cartella con questa app");actions.add("Informazioni app");
        new AlertDialog.Builder(this).setTitle(app.label).setItems(actions.toArray(new String[0]),(d,which)->{
            switch(which){
                case 0:if(onHome)homeItems.remove(app.id);else if(!homeItems.contains(app.id))homeItems.add(app.id);saveHome();showHome();break;
                case 1:chooseFolder(app);break;
                case 2:createFolder(app);break;
                case 3:Intent info=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+app.launch.getComponent().getPackageName()));startActivity(info);break;
            }
        }).show();
    }
    private void createFolder(AppItem app){
        EditText name=new EditText(this);name.setSingleLine();name.setText("Nuova cartella");
        new AlertDialog.Builder(this).setTitle("Nome cartella").setView(name).setNegativeButton("Annulla",null).setPositiveButton("Crea",(d,w)->{
            String id="folder:"+System.currentTimeMillis();folderNames.put(id,name.getText().toString().trim().isEmpty()?"Cartella":name.getText().toString().trim());
            ArrayList<String> list=new ArrayList<>();list.add(app.id);folders.put(id,list);homeItems.remove(app.id);homeItems.add(id);saveHome();showHome();
        }).show();
    }
    private void chooseFolder(AppItem app){
        if(folders.isEmpty()){createFolder(app);return;}
        List<String> ids=new ArrayList<>(folders.keySet());Collections.sort(ids);
        List<String> names=new ArrayList<>();for(String id:ids)names.add(folderNames.getOrDefault(id,"Cartella"));
        new AlertDialog.Builder(this).setTitle("Scegli cartella").setItems(names.toArray(new String[0]),(d,i)->{
            List<String> contents=folders.get(ids.get(i));if(!contents.contains(app.id))contents.add(app.id);
            homeItems.remove(app.id);saveHome();showHome();
        }).setNeutralButton("Nuova cartella",(d,w)->createFolder(app)).show();
    }
    private void folderMenu(String id){
        new AlertDialog.Builder(this).setTitle(folderNames.getOrDefault(id,"Cartella"))
            .setItems(new String[]{"Apri","Rinomina","Elimina cartella (restituisci app alla Home)"},(d,i)->{
                if(i==0)openFolder(id);
                else if(i==1){EditText e=new EditText(this);e.setText(folderNames.get(id));new AlertDialog.Builder(this).setTitle("Rinomina").setView(e).setNegativeButton("Annulla",null).setPositiveButton("Salva",(x,y)->{folderNames.put(id,e.getText().toString());saveHome();showHome();}).show();}
                else{homeItems.remove(id);for(String a:folders.getOrDefault(id,new ArrayList<>()))if(!homeItems.contains(a))homeItems.add(a);folders.remove(id);folderNames.remove(id);saveHome();showHome();}
            }).show();
    }
    private void openFolder(String id){
        base("folder");page.addView(button("‹ Home",this::showHome));addSpace(page,18);
        page.addView(text(folderNames.getOrDefault(id,"Cartella"),25,ink(),true));addSpace(page,15);
        GridLayout g=new GridLayout(this);g.setColumnCount(4);page.addView(g);
        for(String appId:new ArrayList<>(folders.getOrDefault(id,new ArrayList<>()))){AppItem a=byId.get(appId);if(a==null)continue;
            addTile(g,appId,a.label,a,()->open(a),()->new AlertDialog.Builder(this).setTitle(a.label).setItems(new String[]{"Rimuovi dalla cartella"},(d,w)->{
                folders.get(id).remove(appId);if(!homeItems.contains(appId))homeItems.add(appId);saveHome();openFolder(id);
            }).show());
        }
        addSpace(page,20);page.addView(button("+ Aggiungi app",()->chooseFolderApp(id)));
    }
    private void chooseFolderApp(String id){
        loadApps();List<AppItem> options=new ArrayList<>();List<String> names=new ArrayList<>();
        for(AppItem a:installed){if(folders.get(id).contains(a.id))continue;options.add(a);names.add(a.label);}
        new AlertDialog.Builder(this).setTitle("Aggiungi alla cartella").setItems(names.toArray(new String[0]),(d,i)->{
            String appId=options.get(i).id;folders.get(id).add(appId);homeItems.remove(appId);saveHome();openFolder(id);
        }).show();
    }
    private void showSettings(){
        base("settings");page.addView(button("‹ Home",this::showHome));addSpace(page,16);page.addView(text("Impostazioni Home",25,ink(),true));addSpace(page,18);
        page.addView(text("Aspetto icone",16,ink(),true));addSpace(page,8);
        String[] labels={"Originale","Originale con forma Folio (predefinita)","A tema / monocromatica se supportata"};
        int selected=prefs.getInt("icon_mode",1);
        for(int i=0;i<labels.length;i++){final int value=i;TextView b=button((selected==i?"●  ":"○  ")+labels[i],()->{prefs.edit().putInt("icon_mode",value).apply();showSettings();});b.setGravity(Gravity.CENTER_VERTICAL);page.addView(b,lp(-1,58));addSpace(page,7);}
        addSpace(page,13);page.addView(button(dark?"Tema: scuro — passa a chiaro":"Tema: chiaro — passa a scuro",()->{dark=!dark;prefs.edit().putBoolean("dark",dark).apply();showSettings();}));
        addSpace(page,13);page.addView(button("Scegli Folio come Home predefinita",()->{
            if(android.os.Build.VERSION.SDK_INT>=29){android.app.role.RoleManager manager=(android.app.role.RoleManager)getSystemService(Context.ROLE_SERVICE);
                if(manager!=null&&manager.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME)){
                    try{startActivityForResult(manager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME),30);return;}catch(Exception ignored){}
                }
            }
            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
        }));
        addSpace(page,12);page.addView(button("Apri impostazioni Android",()->startActivity(new Intent(Settings.ACTION_SETTINGS))));
        addSpace(page,18);TextView note=text("Folio Home non sostituisce notifiche, Impostazioni rapide o schermata di blocco del sistema. Queste funzionalità appartengono alla futura integrazione Folio OS.",13,secondary(),false);
        page.addView(note);
    }
    private void launchNews(){
        Intent i=pm.getLaunchIntentForPackage("org.folio.news");
        if(i!=null){startActivity(i);return;}
        new AlertDialog.Builder(this).setTitle("Folio News non installato").setMessage("Installa Folio News per aprirlo direttamente dalla Home.").setPositiveButton("OK",null).show();
    }
    private void launchBrowser(){
        try{Intent i=Intent.makeMainSelectorActivity(Intent.ACTION_MAIN,Intent.CATEGORY_APP_BROWSER);startActivity(i);}
        catch(ActivityNotFoundException e){
            try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.wikipedia.org")));}
            catch(ActivityNotFoundException ex){Toast.makeText(this,"Nessun browser disponibile",Toast.LENGTH_SHORT).show();}
        }
    }
    private void launchFiles(){
        try{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,81);}
        catch(ActivityNotFoundException e){Toast.makeText(this,"Gestore file non disponibile",Toast.LENGTH_SHORT).show();}
    }
    private void inflateWidget(LinearLayout hostBox){
        int widgetId=prefs.getInt("widget_id",-1);
        if(widgetId<0){TextView hint=text("Aggiungi un widget dalle app installate",13,secondary(),false);hint.setGravity(Gravity.CENTER);hint.setPadding(dp(8),dp(30),dp(8),dp(30));hostBox.addView(hint,lp(-1,-2));return;}
        AppWidgetProviderInfo info=widgetManager.getAppWidgetInfo(widgetId);
        if(info==null){prefs.edit().remove("widget_id").apply();TextView hint=text("Widget non disponibile · premi + Aggiungi",13,secondary(),false);hint.setGravity(Gravity.CENTER);hint.setPadding(dp(8),dp(28),dp(8),dp(28));hostBox.addView(hint);return;}
        try{
            AppWidgetHostView view=widgetHost.createView(this,widgetId,info);view.setAppWidget(widgetId,info);
            hostBox.addView(view,new LinearLayout.LayoutParams(-1,dp(160)));
            hostBox.setOnLongClickListener(v->{new AlertDialog.Builder(this).setTitle("Rimuovere il widget?")
                .setNegativeButton("Annulla",null).setPositiveButton("Rimuovi",(d,w)->{widgetHost.deleteAppWidgetId(widgetId);prefs.edit().remove("widget_id").apply();showHome();}).show();return true;});
        }catch(Exception e){TextView hint=text("Widget non disponibile: scegli un altro widget",13,secondary(),false);hostBox.addView(hint);}
    }
    private void pickWidget(){
        if(pendingWidgetId>=0)return;
        pendingWidgetId=widgetHost.allocateAppWidgetId();
        Intent i=new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
        i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,pendingWidgetId);
        try{startActivityForResult(i,PICK_WIDGET);}
        catch(ActivityNotFoundException e){chooseWidgetFallback();}
    }
    private void cancelPendingWidget(){
        if(pendingWidgetId>=0){
            try{widgetHost.deleteAppWidgetId(pendingWidgetId);}catch(Exception ignored){}
        }
        pendingWidgetId=-1;
    }
    private void chooseWidgetFallback(){
        List<AppWidgetProviderInfo> options=widgetManager.getInstalledProviders();
        if(options.isEmpty()){
            Toast.makeText(this,"Nessun widget disponibile",Toast.LENGTH_SHORT).show();
            cancelPendingWidget();return;
        }
        List<String> labels=new ArrayList<>();
        for(AppWidgetProviderInfo w:options)labels.add(w.loadLabel(pm));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Seleziona widget")
            .setItems(labels.toArray(new String[0]),(d,which)->{
                AppWidgetProviderInfo selected=options.get(which);
                if(widgetManager.bindAppWidgetIdIfAllowed(pendingWidgetId,selected.provider)){
                    configureWidget(pendingWidgetId);
                }else{
                    Intent req=new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
                    req.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,pendingWidgetId);
                    req.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER,selected.provider);
                    try{startActivityForResult(req,BIND_WIDGET);}
                    catch(Exception ex){
                        cancelPendingWidget();
                        Toast.makeText(this,"Associazione widget non disponibile",Toast.LENGTH_SHORT).show();
                    }
                }
            }).setNegativeButton("Annulla",(d,w)->cancelPendingWidget()).create();
        dialog.setOnCancelListener(d->cancelPendingWidget());
        dialog.show();
    }
    private void configureWidget(int widgetId){
        AppWidgetProviderInfo info=widgetManager.getAppWidgetInfo(widgetId);
        if(info==null){cancelPendingWidget();return;}
        if(info.configure!=null){
            Intent config=new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            config.setComponent(info.configure);
            config.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId);
            try{startActivityForResult(config,CONFIG_WIDGET);return;}
            catch(Exception ex){
                Toast.makeText(this,"Configurazione widget non disponibile",Toast.LENGTH_SHORT).show();
                cancelPendingWidget();return;
            }
        }
        setWidget(widgetId);
    }
    private void setWidget(int id){
        int old=prefs.getInt("widget_id",-1);
        if(old>=0&&old!=id)widgetHost.deleteAppWidgetId(old);
        prefs.edit().putInt("widget_id",id).apply();
        pendingWidgetId=-1;
        showHome();
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==PICK_WIDGET||request==BIND_WIDGET||request==CONFIG_WIDGET){
            if(result!=RESULT_OK){cancelPendingWidget();return;}
            int id=data!=null?data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,pendingWidgetId):pendingWidgetId;
            if(id<0||id!=pendingWidgetId){cancelPendingWidget();return;}
            if(request==CONFIG_WIDGET){setWidget(id);}
            else if(request==BIND_WIDGET){
                if(widgetManager.getAppWidgetInfo(id)!=null)configureWidget(id);
                else cancelPendingWidget();
            }else{configureWidget(id);}
        }
    }
}
