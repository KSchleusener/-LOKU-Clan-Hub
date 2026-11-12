package de.loku.clanhub;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private LinearLayout body;
    private int dp(int n) { return (int)(getResources().getDisplayMetrics().density*n + .5f); }
    private GradientDrawable background(String color, String stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.parseColor(color));
        d.setCornerRadius(dp(16));
        d.setStroke(dp(1),Color.parseColor(stroke));
        return d;
    }
    private TextView text(String s, int size, String color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(Color.parseColor(color));
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private void addText(String s,int size,String color,boolean bold) {
        TextView v=text(s,size,color,bold);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2);
        p.bottomMargin=dp(14); body.addView(v,p);
    }
    private void button(String label, Runnable action) {
        TextView v=text(label,17,"#FFFFFF",true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(14),dp(16),dp(14),dp(16));
        v.setBackground(background("#352055","#9163BC"));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.bottomMargin=dp(11);body.addView(v,p);
        v.setOnClickListener(w->action.run());
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll=new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#120C23"));
        body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(22),dp(26),dp(22),dp(30));
        scroll.addView(body);setContentView(scroll);
        addText("♛ LOKU CLAN HUB",28,"#EBCBFF",true);
        addText("EVENT AUTOCLICKER · TESTVERSION",13,"#C9A5F0",true);
        addText("Ein Event starten, Klickpunkte selbst verschieben oder ein fertiges Profil aus dem Clan importieren. Du entscheidest, wann geklickt wird.",15,"#E3D6F3",false);
        button("① Schwebendes Panel anzeigen",()->{
            if(!Settings.canDrawOverlays(this)) { openOverlaySettings(); return; }
            if(TapService.INSTANCE==null) {toast("Bitte zuerst LOKU Klickhilfe unter Bedienungshilfen aktivieren.");openAccessibilitySettings();return;}
            TapService.INSTANCE.showOverlay();toast("Panel geöffnet. Andere App kann jetzt gestartet werden.");
        });
        button("② Berechtigung: Über anderen Apps anzeigen",this::openOverlaySettings);
        button("③ Bedienungshilfe aktivieren",this::openAccessibilitySettings);
        button("④ Roblox öffnen",()->{
            Intent i=getPackageManager().getLaunchIntentForPackage("com.roblox.client");
            if(i==null){toast("Roblox ist nicht installiert.");return;}
            startActivity(i);
        });
        button("Event-Profil teilen / exportieren",()->{
            String json=getSharedPreferences("loku",0).getString("profile","");
            if(TapService.INSTANCE!=null && TapService.INSTANCE.hasPoints()) json=TapService.INSTANCE.exportProfile();
            if(json.isEmpty()){toast("Erst Klickpunkte im Panel einstellen.");return;}
            Intent i=new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT,"LOKU Event-Profil");
            i.putExtra(Intent.EXTRA_TEXT,json);
            startActivity(Intent.createChooser(i,"Profil als Text versenden"));
        });
        button("Event-Profil importieren",()->{
            EditText edit=new EditText(this);
            edit.setHint("JSON aus dem Clan hier einfügen");
            edit.setMinLines(4);edit.setMaxLines(9);
            new AlertDialog.Builder(this).setTitle("LOKU Profil importieren")
            .setView(edit).setNegativeButton("Abbrechen",null)
            .setPositiveButton("Importieren",(dialog,which)->{
                String json=edit.getText().toString();
                try {
                    JSONObject profile=new JSONObject(json);
                    if(profile.optInt("schemaVersion")!=1 || profile.getJSONArray("points").length()<1)
                        throw new Exception("Ungültiges Profil");
                    if(TapService.INSTANCE!=null && TapService.INSTANCE.hasPanel())
                        TapService.INSTANCE.importProfile(json);
                    else getSharedPreferences("loku",0).edit().putString("profile",json).apply();
                    toast("Profil gespeichert. Panel öffnen, um Punkte zu sehen.");
                } catch(Exception e){toast("Profil konnte nicht importiert werden: "+e.getMessage());}
            }).show();
        });
        button("LOKU Discord öffnen",()->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://discord.gg/EXgZzcmCQ"))));
        addText("Hinweis: Importierte Admin-Profile sind nicht verifiziert. Überprüfe die Positionen auf deinem eigenen Bildschirm. Keine automatische Spiel-Erkennung oder Admin-Synchronisierung in dieser Testversion.",13,"#BEABC9",false);
        addText("Die Bedienungshilfe sendet nur Tippgesten, nachdem du im Panel START gedrückt hast. Keine Roblox-Kontodaten nötig.",13,"#BEABC9",false);
    }
    private void openOverlaySettings(){
        try{startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName())));}
        catch(Exception e){toast("Berechtigung manuell in den Android-Einstellungen einschalten.");}
    }
    private void openAccessibilitySettings(){startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));}
}
