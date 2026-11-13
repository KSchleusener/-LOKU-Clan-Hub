package de.loku.clanhub;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.widget.TextView;
/** Original pet mascot markers; all gestures and timing remain controlled by the user. */
public final class PetBadge extends TextView {
    private final int index;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();
    private static final int[] BASE={0xffa653ee,0xff27cfc9,0xfffc72ae,0xffffc34f,0xff6591ef,0xfffb9177};
    private static final int[] LIGHT={0xffedd4ff,0xffc2ffed,0xffffd8ee,0xffffefb7,0xffd5dfff,0xffffdebd};
    public PetBadge(Context context,int number) {
        super(context);index=number;setText("");setContentDescription("LOKU Pet Klickpunkt "+number);
        setLayerType(LAYER_TYPE_SOFTWARE,null);
    }
    private void circle(Canvas c,float x,float y,float radius,int color){
        p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawCircle(x,y,radius,p);
    }
    private void box(Canvas c,float x,float y,float x2,float y2,float r,int color){
        p.setStyle(Paint.Style.FILL);p.setColor(color);rect.set(x,y,x2,y2);c.drawRoundRect(rect,r,r,p);
    }
    private void tri(Canvas c,float a,float b,float d,float e,float f,float g,int color){
        p.setStyle(Paint.Style.FILL);p.setColor(color);path.reset();
        path.moveTo(a,b);path.lineTo(d,e);path.lineTo(f,g);path.close();c.drawPath(path,p);
    }
    @Override protected void onDraw(Canvas c){
        float s=Math.min(getWidth(),getHeight());if(s<=0)return;
        int t=(index-1)%BASE.length;c.save();c.translate((getWidth()-s)/2,(getHeight()-s)/2);c.scale(s/100f,s/100f);
        circle(c,50,50,49,0xffffd781);circle(c,50,50,45,0xff241535);
        if(t==1||t==4){
            box(c,19,1,40,49,10,BASE[t]);box(c,60,1,81,49,10,BASE[t]);
            box(c,27,8,33,40,4,LIGHT[t]);box(c,67,8,73,40,4,LIGHT[t]);
        }else if(t==3){
            circle(c,23,25,15,BASE[t]);circle(c,77,25,15,BASE[t]);
            circle(c,23,25,6,LIGHT[t]);circle(c,77,25,6,LIGHT[t]);
        }else{
            tri(c,13,11,15,51,48,29,BASE[t]);tri(c,87,11,85,51,52,29,BASE[t]);
            tri(c,20,24,22,41,37,33,LIGHT[t]);tri(c,80,24,78,41,63,33,LIGHT[t]);
        }
        box(c,9,31,91,91,24,BASE[t]);box(c,20,55,80,84,17,LIGHT[t]);
        circle(c,32,53,8,0xff231529);circle(c,68,53,8,0xff231529);
        circle(c,35,49,3,Color.WHITE);circle(c,71,49,3,Color.WHITE);
        tri(c,45,68,55,68,50,74,0xff603454);
        box(c,65,74,97,99,10,0xff1b102b);
        p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(17);p.setFakeBoldText(true);
        c.drawText(Integer.toString(index),81,92,p);p.setFakeBoldText(false);
        c.restore();
    }
}
