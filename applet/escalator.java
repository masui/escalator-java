import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; import netscape.javascript.*; import javax.comm.*; import javax.sound.midi.*; import javax.sound.midi.spi.*; import javax.sound.sampled.*; import javax.sound.sampled.spi.*; import javax.xml.parsers.*; import javax.xml.transform.*; import javax.xml.transform.dom.*; import javax.xml.transform.sax.*; import javax.xml.transform.stream.*; import org.xml.sax.*; import org.xml.sax.ext.*; import org.xml.sax.helpers.*; public class escalator extends BApplet {//
//

//float rspeedfactor = 2.0;
//float RGAPFACTOR = 3.0;

int STRATEGY1 = 0; // 左優先
int STRATEGY2 = 1; // 右優先

float lspeedfactor = 1.0f;
float lgapfactor = 1.0f;
float rspeedfactor = 1.0f;
float rgapfactor = 1.0f;
int strategy = STRATEGY2;

// 1
// 1
// 5    2    2    1    2    1    2      2     1    2    3    2    5
// 1    3    2.5  1    2    2    1      3     1    2    3    1    1
// L    L    L    L    L    L    L      R     R    R    R    R    R
// 2192 2432 2312 2281 2192 3001 2192   2459  2283 2219 2219 1957 1780

// 標準
// 1     1     1     1
// 2     2     1     2
// 2     1     1     1
// 3     1     2     2
// L     L     R     R
// 3392  2912  3063  3783



int MOVING_LOWER = 0;
int WAITING_LOWER = 1;
int ASCENDING = 2;
int UPPER = 3;

float llane = 220.0f;
float rlane = 250.0f;
float escheight = 460.0f;

int WINWIDTH = 500;
int WINHEIGHT = 500;

class Person {
  float x,y;
  int status;
  int lr; // 左レーン(0)/右レーン(1)
  Person(){
    x = 0.0f;
    y = 0.0f;
    status = MOVING_LOWER;
    lr = 0;
  }
}

Person[] persons;

int NPERSONS = 50;

int NSTAIRS = 15;
int STAIRHEIGHT = 60;
int STEPUNIT = 1;
int steppos = 0;
int waiting_l = 0;
int waiting_r = 0;
boolean done = false;

float xspeed = 1.0f;

float parseFloat(String s)
{
  float f[] = splitFloats(s);
  return f[0];
}

void setup(){
  size(WINWIDTH,WINHEIGHT);
  BFont fontA = loadFont("Meta-Bold.vlw.gz");
  textFont(fontA, 20);
  persons = new Person[NPERSONS];
  for(int i=0;i<NPERSONS;i++){
    persons[i] = new Person(); 
  }

  String s;
  s = param("lspeedfactor");
  lspeedfactor = (s == null ? 1.0f : parseFloat(s));
  s = param("rspeedfactor");
  rspeedfactor = (s == null ? 1.0f : parseFloat(s));
  s = param("lgapfactor");
  lgapfactor = (s == null ? 1.0f : parseFloat(s));
  s = param("rgapfactor");
  rgapfactor = (s == null ? 1.0f : parseFloat(s));
}

boolean can_ascend_l(int n)
{
  int i;
  boolean result = true;
  for(i=0;i<n;i++){
    if(persons[i].status == ASCENDING &&
       persons[i].lr == 0 &&
       persons[i].y - persons[n].y < STAIRHEIGHT * lgapfactor){
      return false;
    }
  }
  return true;
}

boolean can_ascend_r(int n)
{
  int i;
  boolean result = true;
  for(i=0;i<n;i++){
    if(persons[i].status == ASCENDING &&
       persons[i].lr == 1 &&
       persons[i].y - persons[n].y < STAIRHEIGHT * rgapfactor){
      return false;
    }
  }
  return true;
}

boolean can_forward_lower(int n)
{
  int i;
  boolean result = true;
  for(i=0;i<n;i++){
    if(persons[i].status == MOVING_LOWER &&
       persons[i].x - persons[n].y < 30.0f){
      return false;
    }
  }
  return true;
}

boolean can_forward_upper(int n)
{
  int i;
  boolean result = true;
  for(i=0;i<NPERSONS;i++){
    if(persons[i].status == UPPER &&
       persons[i].x - persons[n].x > 0.0f &&
       persons[i].x - persons[n].x < 30.0f
       ){
      return false;
    }
  }
  return true;
}

boolean all_upper()
{
  int i;
  boolean result = true;
  for(i=0;i<NPERSONS;i++){
    if(persons[i].status != UPPER) return false;
  }
  return true;
}

int loops = 0;

void loop(){
  int i;
  int x,y;
  
  if(done) return;

  background(213,200,255);

  for(i=0;i<NSTAIRS;i++){
    y = i * STAIRHEIGHT + steppos;
//    line(llane-10+10,WINHEIGHT-y,rlane+10+20+10,WINHEIGHT-y);
    fill(200,200,200);
    strokeWeight(0);
    rect(llane-10+10,WINHEIGHT-y,rlane-llane+40,STAIRHEIGHT/2);
  }
  steppos += STEPUNIT;
  if(steppos >= STAIRHEIGHT){
    steppos = 0;
  }

  for(i=0;i<NPERSONS;i++){ // 駒をひとつずつ進める。
    if(persons[i].status == MOVING_LOWER && // 下で移動中で
            persons[i].x >= llane && // 左レーンまでたどりついたとき
            persons[i].lr == 0
           ){
      // ここで判断が必要。
      if(strategy == STRATEGY1){
        if(waiting_l == 0 || waiting_l < waiting_r){ 
          persons[i].status = WAITING_LOWER;
          waiting_l++;
        }
        else {
          persons[i].lr = 1; // 右で待つことにする
        }
      }
      else if(strategy == STRATEGY2){
        if(waiting_l < waiting_r){ 
          persons[i].status = WAITING_LOWER;
          waiting_l++;
        }
        else {
          persons[i].lr = 1; // 右で待つことにする
        }
      }
    }
    else if(persons[i].status == MOVING_LOWER && // 下で移動中で
            persons[i].x >= rlane && // 右レーンまでたどりついたとき
            persons[i].lr == 1
           ){
      persons[i].status = WAITING_LOWER;
      waiting_r++;
    }
    else if(persons[i].status == MOVING_LOWER &&  // エスカレータにまだ乗ってなくて、
           can_forward_lower(i) // 前に進める場合は
       ){
      // 一歩前に移動する
      persons[i].x += xspeed;
    }
    else if(persons[i].status == UPPER &&  // エスカレータからおりていて
            can_forward_upper(i) // 前に進める場合は
       ){
      // 一歩前に移動する
      persons[i].x += xspeed;
    }
    else if(persons[i].status == ASCENDING && // まだエスカレータに乗っていて
            persons[i].y >= escheight  // エスカレータで上まで到達していれば
      ){
      // おりる
      persons[i].status = UPPER;
      persons[i].y = escheight;
    }
    else if(persons[i].status == WAITING_LOWER && // エスカレータ待ち状態で、
         persons[i].lr == 0 && // 左で待っていて
         can_ascend_l(i) // 上に余地がある
      ){
//    if(steppos == 0){ // エスカレータが来たらそれに乗る!
        persons[i].x = llane;
        persons[i].y = 0.0f;
        persons[i].status = ASCENDING;
        waiting_l--;
//      }
//      else { // エスカレータ待ち
//      }
    }
    else if(persons[i].status == WAITING_LOWER && // エスカレータ待ち状態で、
         persons[i].lr == 1 && // 右で待っていて
         can_ascend_r(i) // 上に余地がある
      ){
//      if(steppos == 0){ // エスカレータが来たらそれに乗る!
        persons[i].x = rlane;
        persons[i].y = 0.0f;
        persons[i].status = ASCENDING;
        waiting_r--;
//      }
//      else { // エスカレータ待ち
//      }
    }
    else if(persons[i].status == ASCENDING //エスカレータに乗っていたら
      ){
      if(persons[i].lr == 0){
        persons[i].y += STEPUNIT * lspeedfactor; // 1段上に移動する
      }
      else {
        persons[i].y += STEPUNIT * rspeedfactor; // 1段上に移動する
      }
    }
  }

  strokeWeight(1);
  for(i=NPERSONS-1;i>=0;i--){
    fill(255,255,0);
    rect(persons[i].x+10,WINHEIGHT-30-persons[i].y,20,20);
    fill(0,0,0);
    text(i+1,persons[i].x+10+3,WINHEIGHT-30-persons[i].y+14);
  }

  if(! all_upper()){
    loops++;
  }
  else {
    done = true;
  }
  fill(0,0,0);
//  text(millis(),20,20);
  text(loops,20,20);
}
}