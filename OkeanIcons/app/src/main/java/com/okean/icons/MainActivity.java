package com.okean.icons;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
  private LinearLayout holder;
  private EditText search;
  private final ArrayList<ResolveInfo> apps = new ArrayList<>();
  private final int bg=Color.rgb(18,14,30),purple=Color.rgb(189,150,255);
  int dp(int p){return (int)(p*getResources().getDisplayMetrics().density+.5);}
  TextView title(String t,int size,int color){
    TextView v=new TextView(this);v.setText(t);v.setTextSize(size);v.setTextColor(color);return v;
  }
  @Override public void onCreate(Bundle b){
    super.onCreate(b);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
    LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(bg);
    root.setPadding(dp(16),dp(14),dp(16),dp(10));setContentView(root);
    TextView brand=title("OKEΛN  ICONS     ⚙",20,purple);brand.setPadding(0,dp(6),0,dp(12));root.addView(brand);
    brand.setOnClickListener(v->{
      try{startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));}
      catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}
    });
    TextClock clock=new TextClock(this);clock.setFormat24Hour("HH:mm");clock.setFormat12Hour("HH:mm");
    clock.setTextSize(50);clock.setTextColor(-1);root.addView(clock);
    TextView hint=title("Твой главный экран. Без названий иконок.",13,0xffbab0cc);
    hint.setPadding(0,0,0,dp(15));root.addView(hint);
    search=new EditText(this);search.setSingleLine(true);search.setTextColor(-1);
    search.setHintTextColor(0xffaaa0ba);search.setHint("Поиск приложений");
    root.addView(search,new LinearLayout.LayoutParams(-1,dp(52)));
    ScrollView scroll=new ScrollView(this);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    holder=new LinearLayout(this);holder.setOrientation(1);scroll.addView(holder);
    search.addTextChangedListener(new TextWatcher(){
      public void beforeTextChanged(CharSequence s,int start,int c,int a){}
      public void onTextChanged(CharSequence s,int start,int before,int count){render(s.toString());}
      public void afterTextChanged(Editable s){}
    });
    reload();
  }
  void reload(){
    apps.clear();
    Intent q=new Intent(Intent.ACTION_MAIN);q.addCategory(Intent.CATEGORY_LAUNCHER);
    for(ResolveInfo i:getPackageManager().queryIntentActivities(q,0)){
      if(i.activityInfo !=null && !i.activityInfo.packageName.equals(getPackageName()))apps.add(i);
    }
    Collections.sort(apps,(a,b)->String.CASE_INSENSITIVE_ORDER.compare(
      String.valueOf(a.loadLabel(getPackageManager())),
      String.valueOf(b.loadLabel(getPackageManager()))));
    render(search.getText().toString());
  }
  void render(String term){
    holder.removeAllViews();
    int width=(getResources().getDisplayMetrics().widthPixels-dp(32))/4;
    LinearLayout row=null;int count=0;
    for(ResolveInfo item:apps){
      String name=String.valueOf(item.loadLabel(getPackageManager()));
      if(!name.toLowerCase(Locale.ROOT).contains(term.toLowerCase(Locale.ROOT)))continue;
      if(count%4==0){
        row=new LinearLayout(this);row.setOrientation(0);
        holder.addView(row,new LinearLayout.LayoutParams(-1,dp(90)));
      }
      LinearLayout cell=new LinearLayout(this);cell.setGravity(Gravity.CENTER);
      cell.setContentDescription(name);cell.setClickable(true);cell.setFocusable(true);
      ImageView icon=new ImageView(this);
      icon.setImageDrawable(item.loadIcon(getPackageManager()));
      cell.addView(icon,new LinearLayout.LayoutParams(dp(54),dp(54)));
      row.addView(cell,new LinearLayout.LayoutParams(width,dp(90)));
      cell.setOnClickListener(v->{
        try{
          Intent launch=new Intent(Intent.ACTION_MAIN);
          launch.addCategory(Intent.CATEGORY_LAUNCHER);
          launch.setClassName(item.activityInfo.packageName,item.activityInfo.name);
          launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
          startActivity(launch);
        }catch(Exception e){Toast.makeText(this,"Не удалось открыть",Toast.LENGTH_SHORT).show();}
      });
      count++;
    }
    if(count==0)holder.addView(title("Приложения не найдены",16,purple));
  }
  @Override public void onResume(){super.onResume();if(search!=null && holder!=null)reload();}
}