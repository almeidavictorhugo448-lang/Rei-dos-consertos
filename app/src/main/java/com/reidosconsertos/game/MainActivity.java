package com.reidosconsertos.game;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.opengl.GLSurfaceView;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends Activity {
    GameRenderer renderer;
    TextView status, task;
    int money=2000, reputation=50, day=1, part=0, job=0, completed=0;
    boolean gpuInstalled=false, ramInstalled=false, diagnosed=false, removed=false, installed=false, tested=false;
    final String[] jobs={"PC gamer não liga","Celular com tela quebrada","Nintendo Switch não carrega","PlayStation superaquecendo","Xbox sem imagem","Joy-Con com drift","Notebook lento","PC gamer sem vídeo"};
    final String[] parts={"Placa de vídeo","Tela OLED","Bateria Switch","Pasta térmica","Porta HDMI","Analógico Joy-Con","SSD NVMe","Memória RAM"};
    final int[] prices={650,240,190,45,160,75,220,180};
    final int[] rewards={520,360,410,390,430,260,340,470};
    int[] stock={0,0,0,0,0,0,0,0};
    int testStep=0;
    SharedPreferences prefs;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().getDecorView().setSystemUiVisibility(5894|1024|512|4096);
        prefs=getSharedPreferences("rei_dos_consertos_save",MODE_PRIVATE);
        loadGame();
        FrameLayout root=new FrameLayout(this);
        GLSurfaceView gl=new GLSurfaceView(this);
        gl.setEGLContextClientVersion(2);
        renderer=new GameRenderer();
        gl.setRenderer(renderer);
        gl.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        final float[] lastTouchX={0f};
        gl.setOnTouchListener((view,event)->{
            if(event.getAction()==android.view.MotionEvent.ACTION_DOWN){lastTouchX[0]=event.getX();return true;}
            if(event.getAction()==android.view.MotionEvent.ACTION_MOVE){
                float dx=event.getX()-lastTouchX[0];lastTouchX[0]=event.getX();
                renderer.rotateCamera(dx*0.008f);return true;
            }
            return true;
        });
        root.addView(gl);
        LinearLayout hud=new LinearLayout(this);
        hud.setOrientation(LinearLayout.VERTICAL);
        hud.setPadding(22,14,22,14);
        hud.setBackgroundColor(0xAA111A25);
        status=label("REI DOS CONSERTOS     CAIXA: R$ "+money+"     ★ "+reputation+"     DIA "+day,17,Color.WHITE);
        hud.addView(status);
        task=label("PEDIDO: "+jobs[job]+"  |  Arraste para girar a oficina e comece pelo diagnóstico.",14,0xFFFFD66B);
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2); tp.topMargin=8; hud.addView(task,tp);
        FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP); root.addView(hud,hp);

        LinearLayout controls=new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(8,8,8,8);
        controls.setBackgroundColor(0xCC111A25);
        String[] names={"DIAGNÓSTICO","PEÇAS","INSTALAR","TESTAR","ENTREGAR","LOJA"};
        for(int i=0;i<names.length;i++){
            final int id=i;
            Button bt=new Button(this); bt.setText(names[i]); bt.setTextSize(11); bt.setTextColor(Color.WHITE);
            bt.setAllCaps(false); bt.setBackground(round(0xFF2469B4));
            LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,58,1); bp.setMargins(4,0,4,0); controls.addView(bt,bp);
            bt.setOnClickListener(v->action(id));
        }
        FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM); root.addView(controls,cp);
        setContentView(root);
    }
    TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    GradientDrawable round(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(18);return d;}
    void update(){
        status.setText("REI DOS CONSERTOS   R$ "+money+"   ★ "+reputation+"   DIA "+day+"   SERVIÇOS "+completed);
        saveGame();
    }
    void message(String text){task.setText(text);}
    void loadGame(){
        money=prefs.getInt("money",2000); reputation=prefs.getInt("rep",50); day=prefs.getInt("day",1);
        job=Math.max(0,Math.min(jobs.length-1,prefs.getInt("job",0)));
        completed=Math.max(0,prefs.getInt("completed",0));
        money=Math.max(0,money); reputation=Math.max(0,Math.min(100,reputation)); day=Math.max(1,day);
        for(int i=0;i<stock.length;i++) stock[i]=prefs.getInt("stock_"+i,0);
    }
    void saveGame(){
        if(prefs==null)return;
        SharedPreferences.Editor e=prefs.edit().putInt("money",money).putInt("rep",reputation).putInt("day",day).putInt("job",job).putInt("completed",completed);
        for(int i=0;i<stock.length;i++)e.putInt("stock_"+i,stock[i]);
        e.apply();
    }
    void showDiagnosis(){
        String[] options={"Cabo / carregador","Limpeza interna","Placa / componente principal"};
        options[(int)(Math.random()*options.length)]=parts[job];
        new AlertDialog.Builder(this).setTitle("Diagnóstico: "+jobs[job])
          .setMessage("Leia o pedido do cliente e escolha a peça que provavelmente está com defeito.")
          .setItems(options,(d,which)->{
              if(options[which].equals(parts[job])){
                  diagnosed=true; removed=false; installed=false; tested=false; testStep=0;
                  part=job;renderer.selected=job;
                  message("Diagnóstico correto! A peça necessária é "+parts[job]+". Confira o estoque em PEÇAS.");
              }else{
                  reputation=Math.max(0,reputation-2);
                  message("Diagnóstico incorreto. O cliente perdeu a confiança. Tente novamente.");
                  update();
              }
          }).show();
    }
    void showParts(){
        String[] rows=new String[parts.length];
        for(int i=0;i<parts.length;i++)rows[i]=parts[i]+"  ·  estoque: "+stock[i]+"  ·  "+(i==job?"NECESSÁRIA":"R$ "+prices[i]);
        new AlertDialog.Builder(this).setTitle("Depósito de peças").setItems(rows,(d,which)->{
            part=which;
            message("Selecionada: "+parts[part]+". Estoque: "+stock[part]+".");
            renderer.selected=part;
        }).setNegativeButton("Fechar",null).show();
    }
    void showShop(){
        String[] rows=new String[parts.length];
        for(int i=0;i<parts.length;i++)rows[i]=parts[i]+"  —  R$ "+prices[i];
        new AlertDialog.Builder(this).setTitle("Loja de componentes · Caixa R$ "+money)
          .setItems(rows,(d,which)->{
              if(money<prices[which]){message("Dinheiro insuficiente para comprar "+parts[which]+".");return;}
              money-=prices[which];stock[which]++;part=which;
              message("Comprado: "+parts[which]+". Estoque agora: "+stock[which]+".");
              update();
          }).setNegativeButton("Fechar",null).show();
    }
    void action(int a){
        if(a==0){showDiagnosis();return;}
        if(a==1){showParts();return;}
        if(a==2){
            if(!diagnosed){message("Faça o diagnóstico antes de instalar qualquer peça.");return;}
            if(part!=job){message("A peça selecionada não corresponde ao defeito. Selecione "+parts[job]+".");return;}
            if(stock[job]<=0){message("Você não tem "+parts[job]+" no estoque. Abra a LOJA.");return;}
            if(!removed){removed=true;message("Peça antiga removida com cuidado. Toque em INSTALAR novamente.");return;}
            stock[job]--;installed=true;gpuInstalled=(job==0);ramInstalled=(job==7);
            renderer.gpu=gpuInstalled;renderer.ram=ramInstalled;
            message(parts[job]+" instalada! Agora faça o TESTE de funcionamento.");update();return;
        }
        if(a==3){
            if(!installed){message("Instale a peça correta antes de testar o aparelho.");return;}
            testStep++;
            if(testStep<3){message("Teste "+testStep+"/3 concluído. Verifique mais uma etapa antes de entregar.");return;}
            tested=true;message("Todos os testes passaram! Aparelho funcionando. Você já pode ENTREGAR ao cliente.");return;
        }
        if(a==4){
            if(!tested){message("Ainda falta testar o aparelho. Não entregue um conserto sem teste.");return;}
            int reward=rewards[job];money+=reward;reputation=Math.min(100,reputation+4);completed++;
            message("Conserto aprovado! +R$ "+reward+" · reputação +4. Próximo cliente: "+jobs[(job+1)%jobs.length]);
            job=(job+1)%jobs.length;diagnosed=false;removed=false;installed=false;tested=false;testStep=0;day++;
            update();return;
        }
        if(a==5){showShop();}
    }

    static class GameRenderer implements GLSurfaceView.Renderer {
        int program, pos, mvpLoc, colorLoc;
        FloatBuffer vertices; ShortBuffer indices;
        float[] projection=new float[16], view=new float[16], model=new float[16], vp=new float[16], mvp=new float[16];
        boolean gpu=false,ram=false; int selected=0;
        volatile float cameraYaw=0.58f;
        void rotateCamera(float delta){cameraYaw+=delta;}
        final float[] cube={-0.5f,-0.5f,-0.5f, 0.5f,-0.5f,-0.5f, 0.5f,0.5f,-0.5f,-0.5f,0.5f,-0.5f,
          -0.5f,-0.5f,0.5f, 0.5f,-0.5f,0.5f, 0.5f,0.5f,0.5f,-0.5f,0.5f,0.5f};
        final short[] ix={0,1,2,0,2,3,4,6,5,4,7,6,0,4,5,0,5,1,3,2,6,3,6,7,1,5,6,1,6,2,0,3,7,0,7,4};
        @Override public void onSurfaceCreated(GL10 gl,EGLConfig cfg){
            GLES20.glClearColor(0.10f,0.14f,0.19f,1); GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            String vs="uniform mat4 uMVP; attribute vec3 aPos; void main(){gl_Position=uMVP*vec4(aPos,1.0);}";
            String fs="precision mediump float; uniform vec4 uColor; void main(){gl_FragColor=uColor;}";
            int v=shader(GLES20.GL_VERTEX_SHADER,vs), f=shader(GLES20.GL_FRAGMENT_SHADER,fs);
            program=GLES20.glCreateProgram();GLES20.glAttachShader(program,v);GLES20.glAttachShader(program,f);GLES20.glLinkProgram(program);
            pos=GLES20.glGetAttribLocation(program,"aPos");mvpLoc=GLES20.glGetUniformLocation(program,"uMVP");colorLoc=GLES20.glGetUniformLocation(program,"uColor");
            ByteBuffer bb=ByteBuffer.allocateDirect(cube.length*4).order(ByteOrder.nativeOrder());vertices=bb.asFloatBuffer();vertices.put(cube).position(0);
            ByteBuffer ib=ByteBuffer.allocateDirect(ix.length*2).order(ByteOrder.nativeOrder());indices=ib.asShortBuffer();indices.put(ix).position(0);
        }
        int shader(int type,String src){int s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,src);GLES20.glCompileShader(s);return s;}
        @Override public void onSurfaceChanged(GL10 gl,int w,int h){GLES20.glViewport(0,0,w,h);Matrix.perspectiveM(projection,0,48,(float)w/Math.max(1,h),0.1f,100f);}
        @Override public void onDrawFrame(GL10 gl){
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);GLES20.glUseProgram(program);
            float distance=10.5f;
            float cameraX=(float)Math.sin(cameraYaw)*distance;
            float cameraZ=(float)Math.cos(cameraYaw)*distance;
            Matrix.setLookAtM(view,0,cameraX,5.2f,cameraZ,0,1.1f,0,0,1,0);Matrix.multiplyMM(vp,0,projection,0,view,0);
            draw(0,-0.18f,0,13,0.35f,10,0.34f,0.38f,0.44f);
            draw(0,2.6f,-4.8f,13,5.2f,0.25f,0.7f,0.62f,0.48f);
            draw(-6.3f,2.5f,0,0.25f,5,9,0.25f,0.31f,0.38f);
            draw(6.3f,2.5f,0,0.25f,5,9,0.25f,0.31f,0.38f);
            // bancada de madeira e estrutura
            draw(0,1.15f,-0.3f,6.8f,0.22f,3.2f,0.48f,0.29f,0.16f);
            for(int x=-2;x<=2;x+=4)for(int z=-1;z<=1;z+=2)draw(x,0.5f,-0.3f+z,0.18f,1.1f,0.18f,0.18f,0.20f,0.23f);
            // gabinete aberto
            draw(-1.15f,2.0f,-0.1f,1.75f,1.75f,1.15f,0.12f,0.15f,0.19f);
            draw(-1.15f,2.0f,0.50f,1.42f,1.4f,0.05f,0.28f,0.32f,0.37f);
            draw(-1.15f,2.0f,0.54f,0.95f,0.9f,0.08f,0.10f,0.45f,0.30f);
            // componentes da placa
            draw(-1.45f,2.05f,0.62f,0.36f,0.12f,0.36f,0.75f,0.75f,0.78f);
            draw(-0.85f,2.05f,0.62f,0.22f,0.1f,0.32f,0.75f,0.75f,0.78f);
            if(ram)draw(-0.55f,2.25f,0.58f,0.12f,0.65f,0.18f,0.1f,0.85f,0.45f);else draw(-0.55f,2.25f,0.58f,0.12f,0.65f,0.18f,0.85f,0.65f,0.12f);
            if(gpu)draw(-1.2f,1.68f,0.6f,1.05f,0.22f,0.35f,0.18f,0.52f,0.92f);else draw(-1.2f,1.68f,0.6f,0.75f,0.18f,0.25f,0.75f,0.24f,0.18f);
            // monitor e ferramentas
            draw(2.05f,2.35f,-1.05f,1.8f,1.15f,0.12f,0.035f,0.055f,0.08f);
            draw(2.05f,2.35f,-0.96f,1.58f,0.92f,0.04f,0.12f,0.48f,0.72f);
            draw(2.05f,1.65f,-1.02f,0.18f,0.65f,0.18f,0.25f,0.27f,0.30f);
            draw(2.0f,1.3f,0.0f,1.5f,0.08f,0.55f,0.08f,0.09f,0.11f);
            draw(0.2f,1.34f,-1.25f,0.25f,0.42f,0.25f,0.95f,0.55f,0.08f);
            // prateleira e caixas de peças
            draw(-4.8f,3.1f,-3.7f,2.1f,0.12f,1.0f,0.40f,0.25f,0.14f);
            draw(-5.0f,3.55f,-3.7f,0.55f,0.65f,0.65f,0.15f,0.42f,0.30f);
            draw(-4.35f,3.55f,-3.7f,0.55f,0.65f,0.65f,0.18f,0.20f,0.24f);
            // luzes e decoração
            draw(0,4.8f,-2.2f,3.4f,0.08f,0.25f,0.95f,0.83f,0.55f);
        }
        void draw(float x,float y,float z,float sx,float sy,float sz,float r,float g,float b){
            Matrix.setIdentityM(model,0);Matrix.translateM(model,0,x,y,z);Matrix.scaleM(model,0,sx,sy,sz);Matrix.multiplyMM(mvp,0,vp,0,model,0);
            GLES20.glUniformMatrix4fv(mvpLoc,1,false,mvp,0);GLES20.glUniform4f(colorLoc,r,g,b,1);
            GLES20.glEnableVertexAttribArray(pos);GLES20.glVertexAttribPointer(pos,3,GLES20.GL_FLOAT,false,0,vertices);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES,ix.length,GLES20.GL_UNSIGNED_SHORT,indices);GLES20.glDisableVertexAttribArray(pos);
        }
    }
}