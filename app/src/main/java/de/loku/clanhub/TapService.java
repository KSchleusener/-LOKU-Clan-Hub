package de.loku.clanhub;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

public class TapService extends AccessibilityService {
    public static TapService INSTANCE;
    private final ArrayList<Dot> dots=new ArrayList<>();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private WindowManager manager;
    private View panel;
    private WindowManager.LayoutParams panelParams;
    private TextView status;
    private boolean running=false;
    private int intervalMs=700;
    private int current=0;
    private int width,height;

    private class Dot {
        TextView view;
        WindowManager.LayoutParams params;
        Dot(TextView v,WindowManager.LayoutParams p){view=v;params=p;}
    }
    private int dp(int n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
    private GradientDrawable round(String color,int r,String border){
        GradientDrawable d=new GradientDrawable();
        d.setColor(Color.parseColor(color));d.setCornerRadius(dp(r));
        d.setStroke(dp(2),Color.parseColor(border));return d;
    }
    private int type(){return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;}
    private WindowManager.LayoutParams params(int w,int h,int x,int y){
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(
            w,h,type(),WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.TOP|Gravity.LEFT;p.x=x;p.y=y;return p;
    }
    @Override public void onServiceConnected(){
        super.onServiceConnected();
        INSTANCE=this;
        manager=(WindowManager)getSystemService(Context.WINDOW_SERVICE);
        width=getResources().getDisplayMetrics().widthPixels;
        height=getResources().getDisplayMetrics().heightPixels;
    }
    public boolean hasPanel(){return panel!=null;}
    public boolean hasPoints(){return !dots.isEmpty();}
    public void showOverlay(){
        if(panel!=null)return;
        if(!Settings.canDrawOverlays(this)){
            Toast.makeText(this,"Bitte Overlay erlauben",Toast.LENGTH_SHORT).show();return;
        }
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(10),dp(9),dp(10),dp(9));
        box.setBackground(round("#25163F",16,"#BD6AFF"));
        TextView title=label("♛ LOKU · EVENT PANEL",15,"#FFFFFF");
        title.setPadding(dp(8),dp(8),dp(8),dp(8));
        box.addView(title);
        title.setOnTouchListener(new View.OnTouchListener(){
            float startX,startY;int px,py;
            public boolean onTouch(View v,MotionEvent event){
                if(event.getAction()==MotionEvent.ACTION_DOWN){
                    startX=event.getRawX();startY=event.getRawY();
                    px=panelParams.x;py=panelParams.y;return true;
                }
                if(event.getAction()==MotionEvent.ACTION_MOVE){
                    panelParams.x=px+(int)(event.getRawX()-startX);
                    panelParams.y=py+(int)(event.getRawY()-startY);
                    manager.updateViewLayout(panel,panelParams);return true;
                }
                return true;
            }
        });
        status=label("Klickhilfe bereit · "+intervalMs+" ms",12,"#D6BBFF");
        box.addView(status);
        LinearLayout line1=new LinearLayout(this);box.addView(line1);
        action(line1,"+ Punkt",()->addDot(width/2,height/2));
        action(line1,"− Punkt",()->removeLast());
        LinearLayout line2=new LinearLayout(this);box.addView(line2);
        action(line2,"START",this::start);
        action(line2,"STOP",this::stop);
        LinearLayout line3=new LinearLayout(this);box.addView(line3);
        action(line3,"Intervall +",()->{intervalMs=Math.min(2000,intervalMs+100);save();refresh();});
        action(line3,"Intervall −",()->{intervalMs=Math.max(250,intervalMs-100);save();refresh();});
        LinearLayout line4=new LinearLayout(this);box.addView(line4);
        action(line4,"Speichern",()->{save();toast("Profil lokal gespeichert");});
        action(line4,"Schließen",this::closePanel);
        panelParams=params(dp(290),-2,dp(18),dp(75));
        panel=box;manager.addView(panel,panelParams);
        String profile=getSharedPreferences("loku",0).getString("profile","");
        if(!profile.isEmpty()){
            try{importProfile(profile);}catch(Exception e){addDot(width/2,height/3);}
        }else{
            addDot(width/2,height/3);addDot(width/2,2*height/3);
        }
        refresh();
    }
    private TextView label(String text,int size,String color){
        TextView t=new TextView(this);t.setText(text);t.setTextSize(size);
        t.setTextColor(Color.parseColor(color));t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        t.setGravity(Gravity.CENTER);return t;
    }
    private void action(LinearLayout r,String name,Runnable callback){
        TextView t=label(name,12,"#FFFFFF");
        t.setBackground(round("#60378B",9,"#9D72C8"));
        t.setPadding(dp(3),dp(12),dp(3),dp(12));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);
        p.setMargins(dp(2),dp(3),dp(2),dp(3));r.addView(t,p);
        t.setOnClickListener(v->callback.run());
    }
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_SHORT).show();}
    private void addDot(int cx,int cy){
        int diameter=dp(48);
        TextView v=label(String.valueOf(dots.size()+1),19,"#FFFFFF");
        v.setBackground(round("#A958F0",48,"#FFF0A0"));
        WindowManager.LayoutParams p=params(diameter,diameter,
            Math.max(0,Math.min(width-diameter,cx-diameter/2)),
            Math.max(0,Math.min(height-diameter,cy-diameter/2)));
        Dot d=new Dot(v,p);dots.add(d);manager.addView(v,p);
        v.setOnTouchListener(new View.OnTouchListener(){
            float sx,sy;int x,y;
            public boolean onTouch(View view,MotionEvent event){
                if(event.getAction()==MotionEvent.ACTION_DOWN){
                    sx=event.getRawX();sy=event.getRawY();x=p.x;y=p.y;return true;
                }
                if(event.getAction()==MotionEvent.ACTION_MOVE){
                    p.x=Math.max(0,Math.min(width-diameter,x+(int)(event.getRawX()-sx)));
                    p.y=Math.max(0,Math.min(height-diameter,y+(int)(event.getRawY()-sy)));
                    manager.updateViewLayout(v,p);return true;
                }
                if(event.getAction()==MotionEvent.ACTION_UP){save();return true;}
                return true;
            }
        });
        save();refresh();
    }
    private void removeLast(){
        if(dots.size()<2){toast("Mindestens ein Punkt nötig");return;}
        Dot d=dots.remove(dots.size()-1);manager.removeView(d.view);save();refresh();
    }
    private final Runnable tick=new Runnable(){
        @Override public void run(){
            if(!running)return;
            if(!dots.isEmpty()){
                Dot d=dots.get(current++ % dots.size());
                Path path=new Path();
                path.moveTo(d.params.x+dp(24),d.params.y+dp(24));
                GestureDescription g=new GestureDescription.Builder()
                    .addStroke(new GestureDescription.StrokeDescription(path,0,55)).build();
                dispatchGesture(g,null,null);
            }
            handler.postDelayed(this,intervalMs);
        }
    };
    private void start(){
        if(dots.isEmpty()){toast("Bitte zuerst einen Punkt setzen");return;}
        if(running)return;
        save();running=true;current=0;
        for(Dot d:dots)d.view.setVisibility(View.INVISIBLE);
        refresh();handler.post(tick);
    }
    private void stop(){
        running=false;handler.removeCallbacks(tick);
        for(Dot d:dots)d.view.setVisibility(View.VISIBLE);
        refresh();
    }
    private void refresh(){
        if(status!=null)status.setText((running?"LÄUFT · ":"Bereit · ")+
            dots.size()+" Punkte · "+intervalMs+" ms");
    }
    public String exportProfile(){
        JSONObject root=new JSONObject();JSONArray pts=new JSONArray();
        try{
            root.put("schemaVersion",1);root.put("name","LOKU Event");
            root.put("intervalMs",intervalMs);
            for(Dot d:dots){
                JSONObject p=new JSONObject();
                p.put("x",Math.max(0,Math.min(1,(d.params.x+dp(24))/(double)width)));
                p.put("y",Math.max(0,Math.min(1,(d.params.y+dp(24))/(double)height)));
                pts.put(p);
            }
            root.put("points",pts);
        }catch(Exception ignored){}
        return root.toString();
    }
    private void save(){
        if(dots.isEmpty())return;
        getSharedPreferences("loku",0).edit().putString("profile",exportProfile()).apply();
    }
    public void importProfile(String json) throws Exception {
        JSONObject root=new JSONObject(json);
        if(root.optInt("schemaVersion")!=1)throw new Exception("Nicht unterstütztes Profil");
        JSONArray arr=root.getJSONArray("points");
        if(arr.length()<1||arr.length()>30)throw new Exception("1 bis 30 Punkte erlaubt");
        int delay=root.optInt("intervalMs",700);
        if(delay<250||delay>2000)throw new Exception("Intervall 250–2000 ms");
        // Validate everything before changing the active point set.
        int[] xs=new int[arr.length()],ys=new int[arr.length()];
        for(int i=0;i<arr.length();i++){
            JSONObject p=arr.getJSONObject(i);
            double x=p.getDouble("x"),y=p.getDouble("y");
            if(!Double.isFinite(x)||!Double.isFinite(y)||x<0||x>1||y<0||y>1)
                throw new Exception("Punktkoordinaten ungültig");
            xs[i]=(int)(x*width);ys[i]=(int)(y*height);
        }
        stop();
        for(Dot d:dots)manager.removeView(d.view);
        dots.clear();intervalMs=delay;
        for(int i=0;i<xs.length;i++)addDot(xs[i],ys[i]);
        save();refresh();
    }
    private void closePanel(){
        stop();save();
        for(Dot d:dots)manager.removeView(d.view);
        dots.clear();
        if(panel!=null){manager.removeView(panel);panel=null;}
        status=null;
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event){}
    @Override public void onInterrupt(){stop();}
    @Override public void onDestroy(){
        if(panel!=null)closePanel();
        handler.removeCallbacks(tick);
        INSTANCE=null;super.onDestroy();
    }
}
