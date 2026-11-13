package de.loku.clanhub;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/** Records touches on its own transparent training surface, never reads Roblox touches. */
public final class MacroRecorder {
    private static final int MAX_ACTIONS=300;
    private final AccessibilityService service;
    private final WindowManager manager;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ArrayList<JSONObject> recorded=new ArrayList<>();
    private final Point screen=new Point();
    private JSONArray profiles;
    private LinearLayout menu;
    private FrameLayout capture;
    private View stopBubble;
    private TextView selectionLabel;
    private boolean recording,playing,looping;
    private int chosen=-1,index,session;
    private long lastEnd,pressedAt,lastSample;
    private float pressedX,pressedY;
    private JSONArray trajectory;
    private JSONArray playback;
    public MacroRecorder(AccessibilityService owner,WindowManager wm){
        service=owner;manager=wm;measure();
        try{profiles=new JSONArray(service.getSharedPreferences("loku-macros",Context.MODE_PRIVATE).getString("saved","[]"));}
        catch(Exception e){profiles=new JSONArray();}
        if(profiles.length()>0)chosen=profiles.length()-1;
    }
    private void measure(){manager.getDefaultDisplay().getRealSize(screen);}
    private int dp(int p){return (int)(p*service.getResources().getDisplayMetrics().density+.5f);}
    private void toast(String text){Toast.makeText(service,text,Toast.LENGTH_LONG).show();}
    private TextView button(String text,int color){
        TextView v=new TextView(service);v.setText(text);v.setTextSize(14);v.setGravity(Gravity.CENTER);
        v.setTextColor(Color.WHITE);v.setPadding(dp(8),dp(10),dp(8),dp(10));
        GradientDrawable bg=new GradientDrawable();bg.setColor(color);bg.setCornerRadius(dp(12));bg.setStroke(dp(1),0xff9d6dcc);v.setBackground(bg);
        return v;
    }
    private WindowManager.LayoutParams layout(int w,int h,int gravity,int x,int y){
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(w,h,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);
        p.gravity=gravity;p.x=x;p.y=y;return p;
    }
    private void add(LinearLayout box,String title,Runnable action){
        TextView t=button(title,0xff533478);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(4);
        box.addView(t,p);t.setOnClickListener(v->action.run());
    }
    private String chosenName(){
        try{return profiles.getJSONObject(chosen).optString("name","Aufnahme");}
        catch(Exception e){return "keine Aufnahme";}
    }
    public void show(){
        if(recording)return;
        if(menu!=null){close();return;}
        menu=new LinearLayout(service);menu.setOrientation(LinearLayout.VERTICAL);menu.setPadding(dp(9),dp(9),dp(9),dp(9));
        GradientDrawable bg=new GradientDrawable();bg.setColor(0xff241338);bg.setCornerRadius(dp(14));menu.setBackground(bg);
        selectionLabel=button("♛ LOKU MAKROS · "+chosenName(),0xff7044a0);menu.addView(selectionLabel);
        add(menu,"● Aufnehmen (Tippen / Wischen)",this::record);
        add(menu,"▶ Makro abspielen",this::play);
        add(menu,"■ Abspielen stoppen",this::stop);
        add(menu,"🔁 Endlosschleife: "+(looping?"AN":"AUS"),()->{looping=!looping;close();show();});
        add(menu,"📁 Nächste Aufnahme",()->{
            if(profiles.length()==0){toast("Keine Aufnahme gespeichert");return;}
            chosen=(chosen+1)%profiles.length();selectionLabel.setText("♛ LOKU MAKROS · "+chosenName());
        });
        add(menu,"📤 Makro teilen",this::share);
        add(menu,"🗑 Profil löschen",this::delete);
        add(menu,"Schließen",this::close);
        manager.addView(menu,layout(dp(246),-2,Gravity.TOP|Gravity.RIGHT,dp(10),dp(80)));
    }
    private void close(){if(menu!=null){manager.removeView(menu);menu=null;selectionLabel=null;}}
    private void persist(){service.getSharedPreferences("loku-macros",Context.MODE_PRIVATE).edit().putString("saved",profiles.toString()).apply();}
    private void delete(){
        if(chosen<0)return;
        profiles.remove(chosen);chosen=profiles.length()-1;persist();close();show();
    }
    private void share(){
        if(chosen<0)return;
        try{
            Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,profiles.getJSONObject(chosen).toString(2));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Intent chooser=Intent.createChooser(i,"LOKU Makro teilen");chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            service.startActivity(chooser);
        }catch(Exception e){toast("Teilen nicht möglich");}
    }
    private void record(){
        stop();close();measure();recorded.clear();recording=true;lastEnd=SystemClock.uptimeMillis();
        capture=new FrameLayout(service);capture.setBackgroundColor(0x240c071c);
        View pad=new View(service);capture.addView(pad,new FrameLayout.LayoutParams(-1,-1));
        pad.setOnTouchListener((v,event)->touch(event));
        TextView done=button("■ STOP + SPEICHERN",0xff75254c);
        FrameLayout.LayoutParams top=new FrameLayout.LayoutParams(dp(198),dp(52),Gravity.TOP|Gravity.RIGHT);
        top.setMargins(0,dp(43),dp(11),0);capture.addView(done,top);done.setOnClickListener(v->endRecord(true));
        TextView warning=button("Aufnahmefeld: Hier Klicks und Wischbewegungen vormachen.\nRoblox erhält während der Aufnahme keine Berührungen.",0xbb28173e);
        FrameLayout.LayoutParams bottom=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);
        bottom.setMargins(dp(11),0,dp(11),dp(18));capture.addView(warning,bottom);
        manager.addView(capture,layout(-1,-1,Gravity.TOP|Gravity.LEFT,0,0));
        handler.postDelayed(()->{if(recording)endRecord(true);},600000);
    }
    private void append(float x,float y){
        try{JSONObject p=new JSONObject();p.put("x",Math.max(0,Math.min(1,x/Math.max(1,screen.x))));
            p.put("y",Math.max(0,Math.min(1,y/Math.max(1,screen.y))));trajectory.put(p);
        }catch(Exception ignored){}
    }
    private boolean touch(MotionEvent e){
        if(!recording)return true;
        long now=SystemClock.uptimeMillis();int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){
            pressedAt=now;lastSample=now;pressedX=e.getRawX();pressedY=e.getRawY();
            trajectory=new JSONArray();append(pressedX,pressedY);return true;
        }
        if(action==MotionEvent.ACTION_MOVE&&trajectory!=null){
            if(now-lastSample>=25&&trajectory.length()<80){append(e.getRawX(),e.getRawY());lastSample=now;}
            return true;
        }
        if((action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)&&trajectory!=null){
            if(action==MotionEvent.ACTION_UP&&recorded.size()<MAX_ACTIONS){
                boolean swipe=Math.hypot(e.getRawX()-pressedX,e.getRawY()-pressedY)>dp(18);
                if(swipe)append(e.getRawX(),e.getRawY());
                else {trajectory=new JSONArray();append(pressedX,pressedY);}
                try{
                    JSONObject step=new JSONObject();
                    step.put("wait",Math.max(0,Math.min(60000,pressedAt-lastEnd)));
                    step.put("duration",swipe?Math.max(80,Math.min(10000,now-pressedAt)):55);
                    step.put("path",trajectory);recorded.add(step);lastEnd=now;
                }catch(Exception ignored){}
                if(recorded.size()>=MAX_ACTIONS)endRecord(true);
            }
            trajectory=null;return true;
        }
        return true;
    }
    private void endRecord(boolean save){
        if(!recording)return;recording=false;
        if(capture!=null){manager.removeView(capture);capture=null;}
        if(save&&!recorded.isEmpty()){
            try{
                JSONObject p=new JSONObject();p.put("name","Event "+new SimpleDateFormat("dd.MM. HH:mm",Locale.GERMANY).format(new Date()));
                p.put("format","LOKU-RECORD-1");JSONArray steps=new JSONArray();
                for(JSONObject s:recorded)steps.put(s);p.put("steps",steps);
                profiles.put(p);while(profiles.length()>20)profiles.remove(0);
                chosen=profiles.length()-1;persist();toast("Gespeichert: "+recorded.size()+" Gesten");
            }catch(Exception e){toast("Speichern fehlgeschlagen");}
        }
        recorded.clear();show();
    }
    private void play(){
        if(playing||recording)return;
        try{if(chosen<0)throw new Exception();playback=profiles.getJSONObject(chosen).getJSONArray("steps");if(playback.length()<1)throw new Exception();}
        catch(Exception e){toast("Erst eine Aufnahme erstellen");return;}
        close();measure();playing=true;index=0;int id=++session;
        stopBubble=button("■ LOKU STOP",0xff812b55);stopBubble.setOnClickListener(v->stop());
        manager.addView(stopBubble,layout(dp(140),dp(52),Gravity.BOTTOM|Gravity.LEFT,dp(10),dp(55)));
        handler.postDelayed(()->next(id),500);
    }
    private void next(int id){
        if(!playing||session!=id)return;
        if(index>=playback.length()){
            if(looping)index=0;else{stop();return;}
        }
        try{
            JSONObject step=playback.getJSONObject(index++);
            long wait=Math.max(0,Math.min(60000,step.optLong("wait",0)));
            handler.postDelayed(()->dispatch(id,step),wait);
        }catch(Exception e){stop();}
    }
    private void dispatch(int id,JSONObject step){
        if(!playing||session!=id)return;
        try{
            JSONArray pathItems=step.getJSONArray("path");if(pathItems.length()<1||pathItems.length()>80)throw new Exception();
            Path path=new Path();
            for(int i=0;i<pathItems.length();i++){
                JSONObject pt=pathItems.getJSONObject(i);double x=pt.getDouble("x"),y=pt.getDouble("y");
                if(!Double.isFinite(x)||!Double.isFinite(y)||x<0||x>1||y<0||y>1)throw new Exception();
                float xx=(float)x*screen.x, yy=(float)y*screen.y;
                if(i==0)path.moveTo(xx,yy);else path.lineTo(xx,yy);
            }
            long duration=Math.max(50,Math.min(10000,step.optLong("duration",55)));
            GestureDescription g=new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(path,0,duration)).build();
            if(!service.dispatchGesture(g,new AccessibilityService.GestureResultCallback(){
                @Override public void onCompleted(GestureDescription desc){if(playing&&id==session)handler.post(()->next(id));}
                @Override public void onCancelled(GestureDescription desc){if(playing&&id==session)handler.post(()->next(id));}
            },handler)){stop();toast("Android verweigert die Geste");}
        }catch(Exception e){stop();toast("Makro konnte nicht abgespielt werden");}
    }
    public void stop(){playing=false;session++;if(stopBubble!=null){manager.removeView(stopBubble);stopBubble=null;}}
    public void shutdown(){stop();if(recording)endRecord(false);close();handler.removeCallbacksAndMessages(null);}
}
