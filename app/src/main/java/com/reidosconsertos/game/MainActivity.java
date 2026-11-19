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
import android.widget.ImageView;
import android.widget.ScrollView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import java.net.URL;
import java.net.HttpURLConnection;
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
    GLSurfaceView glView;
    TextView status, task;
    int money=2000, reputation=50, day=1, part=0, job=0, completed=0;
    boolean gpuInstalled=false, ramInstalled=false, diagnosed=false, removed=false, installed=false, tested=false;
    final String[] jobs={"PC gamer não liga","Celular com tela quebrada","Nintendo Switch não carrega","PlayStation superaquecendo","Xbox sem imagem","Joy-Con com drift","Notebook lento","PC gamer sem vídeo"};
    final int[] rewards={520,360,410,390,430,260,340,470};
    int[] stock=new int[26];
    int testStep=0;
    SharedPreferences prefs;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().getDecorView().setSystemUiVisibility(5894|1024|512|4096);
        prefs=getSharedPreferences("rei_dos_consertos_save",MODE_PRIVATE);
        loadGame();
        FrameLayout root=new FrameLayout(this);
        GLSurfaceView gl=new GLSurfaceView(this);
        glView=gl;
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
    @Override protected void onPause(){
        saveGame();
        if(glView!=null)glView.onPause();
        super.onPause();
    }
    @Override protected void onResume(){
        super.onResume();
        if(glView!=null)glView.onResume();
    }
    void update(){
        status.setText("REI DOS CONSERTOS   R$ "+money+"   ★ "+reputation+"   DIA "+day+"   SERVIÇOS "+completed);
        saveGame();
    }
    void message(String text){task.setText(text);}
    void loadGame(){
        money=prefs.getInt("money",2000); reputation=prefs.getInt("rep",50); day=prefs.getInt("day",1);
        job=Math.max(0,Math.min(jobs.length-1,prefs.getInt("job",0)));
        part=job;
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
    final String[] parts={"Placa de vídeo","Tela OLED","Bateria Switch","Pasta térmica","Conector HDMI","Analógico Joy-Con","SSD NVMe","Memória RAM",
        "Processador","Placa-mãe","Fonte ATX","Cooler de CPU","Gabinete","Memória RAM DDR5","Tela LCD de celular","Bateria de celular","Câmera traseira","Câmera frontal","Conector de carga USB-C","Alto-falante de celular","Controle sem fio","Bateria de controle","Botões e gatilhos","Leitor de discos","Ventoinha de console","Pasta térmica para console"};
    final int[] prices={650,240,190,45,160,75,220,180,520,430,280,130,350,260,180,110,140,95,65,55,220,70,85,190,120,35};
    final String[] partDetails={
        "GPU gamer · PCI Express · refrigeração dupla","Display OLED completo · flex e touch","Bateria Li-ion HAC-003 · 4310 mAh","Pasta térmica · seringa 3,5 g","Conector HDMI · reposição de vídeo","Módulo analógico Joy-Con · correção de drift","SSD M.2 NVMe · armazenamento rápido","Memória DDR4 · módulo de desktop",
        "CPU desktop · alto desempenho","Socket AM4 · chipset e slots","Fonte ATX · alimentação estável","Dissipador e ventoinha","Estrutura para montagem gamer","Módulo DDR5 · alta velocidade",
        "Display LCD com touch","Bateria interna recarregável","Módulo de câmera principal","Câmera frontal e sensores","Flex de carga e dados","Módulo de áudio interno",
        "Controle Bluetooth para console","Bateria recarregável de controle","Kit de botões, gatilhos e membranas","Unidade óptica de reposição","Fan interno para refrigeração","Composto térmico para APU"};
    final int[] partCategory={0,1,2,0,2,2,0,0,0,0,0,0,0,0,1,1,1,1,1,1,2,2,2,2,2,2};
    final String[] partImages={
        "https://images.pexels.com/photos/6704945/pexels-photo-6704945.jpeg?auto=compress&dpr=1&h=750&w=1260",
        "https://media2.gsm55.com/media/catalog/product/images/l/lcd-hard-14r/neutre/lcd-hard-14r-1.jpg",
        "https://static.fnac-static.com/multimedia/Images/AB/AB/A8/C0/12626091-1505-1505-1/tsp20190902145028/HAC-003-3-7V-4310mAh-Charge-Batterie-de-remplacement-integree-pour-Nintendo-Switch-Controller.jpg",
        "https://www.jumbo-computer.com/cdn/shop/files/7166oBCV_PL._AC_UF894_1000_QL80.jpg?v=1695700420",
        "https://instock.net/cdn/shop/files/NT9753_2.jpg?v=1771701742&width=800",
        "https://images-na.ssl-images-amazon.com/images/I/61JPQKrfaqL.jpg",
        "https://dmassets.micron.com/is/image/microntechnology/product-4600-right-white%3A3-2-all-others?dpr=off&ts=1740701906067",
        "https://s.alicdn.com/%40sc04/kf/H0e528f597be448bfa588e230403705ff7/FURY-16GB-2x8GB-DDR4-3200-Ram.jpg"
        // As demais peças usam fotos de referência da mesma família de componentes.\n    };
    int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    void loadPartPhoto(ImageView target,String url){
        target.setBackground(round(0xFF263445));
        new Thread(()->{
            HttpURLConnection c=null;
            try{c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(7000);c.setReadTimeout(9000);c.setInstanceFollowRedirects(true);c.connect();
                Bitmap bmp=BitmapFactory.decodeStream(c.getInputStream());
                if(bmp!=null)new Handler(Looper.getMainLooper()).post(()->target.setImageBitmap(bmp));
            }catch(Exception ignored){}finally{if(c!=null)c.disconnect();}
        }).start();
    }
    void showCatalog(boolean shop){
        if(shop){showShop();return;}
        showPartsCatalog();
    }
    void showPartsCatalog(){
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(14),dp(8),dp(14),dp(8));page.setBackgroundColor(0xFF111A25);
        page.addView(label("SEU ESTOQUE · PEÇAS E COMPONENTES",13,0xFF9DB9D5));
        ScrollView scroll=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(0,dp(8),0,dp(8));scroll.addView(list);
        for(int i=0;i<parts.length;i++){final int index=i;if(stock[i]<=0)continue;addPartCard(list,index,false,null);}
        page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView footer=label("CAIXA DISPONÍVEL: R$ "+money,14,0xFFFFD166);page.addView(footer);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("DEPÓSITO / INVENTÁRIO").setView(page).setNegativeButton("VOLTAR",null).create();
        dialog.show();dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels*0.92),(int)(getResources().getDisplayMetrics().heightPixels*0.84));
    }
    void addPartCard(LinearLayout list,int i,boolean shop,TextView[] footer){
        final int index=i;
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.HORIZONTAL);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp(9),dp(9),dp(9),dp(9));card.setBackground(round(0xFF202E3D));
        LinearLayout.LayoutParams rowP=new LinearLayout.LayoutParams(-1,-2);rowP.bottomMargin=dp(9);list.addView(card,rowP);
        ImageView photo=new ImageView(this);photo.setScaleType(ImageView.ScaleType.CENTER_CROP);LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(92),dp(88));ip.rightMargin=dp(10);card.addView(photo,ip);loadPartPhoto(photo,partImages[index%partImages.length]);
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setGravity(Gravity.CENTER_VERTICAL);card.addView(info,new LinearLayout.LayoutParams(0,-2,1));
        info.addView(label(parts[index],14,Color.WHITE));TextView detail=label(partDetails[index],10,0xFFB5C5D5);detail.setPadding(0,dp(3),0,dp(4));info.addView(detail);
        TextView price=label(shop?"R$ "+prices[index]:"NO ESTOQUE: "+stock[index],13,shop?0xFFFFD166:0xFF77E0A0);info.addView(price);
        Button act=new Button(this);act.setText(shop?"COMPRAR":"SELECIONAR");act.setTextSize(10);act.setTextColor(Color.WHITE);act.setAllCaps(false);act.setBackground(round(shop?0xFF247A55:0xFF2469B4));
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(40));ap.topMargin=dp(5);info.addView(act,ap);
        act.setOnClickListener(v->{
            if(shop){
                if(money<prices[index]){message("Saldo insuficiente para comprar "+parts[index]+".");return;}
                money-=prices[index];stock[index]++;part=index;message("Compra realizada: "+parts[index]+". Agora há "+stock[index]+" no estoque.");update();
                price.setText("R$ "+prices[index]+"  ·  ESTOQUE: "+stock[index]);
                act.animate().scaleX(1.08f).scaleY(1.08f).setDuration(100).withEndAction(()->act.animate().scaleX(1f).scaleY(1f).setDuration(120)).start();
            }else{part=index;renderer.selected=index;message("Peça selecionada: "+parts[index]+" · estoque: "+stock[index]);}
        });
        card.setAlpha(0f);card.setTranslationY(dp(12));card.animate().alpha(1f).translationY(0).setDuration(220).start();
    }
    void showShop(){
        LinearLayout desktop=new LinearLayout(this);
        desktop.setOrientation(LinearLayout.VERTICAL);
        desktop.setPadding(dp(14),dp(12),dp(14),dp(12));
        GradientDrawable wallpaper=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFF102A43,0xFF174B68,0xFF101923});
        wallpaper.setCornerRadius(dp(16)); desktop.setBackground(wallpaper);
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(10),dp(8),dp(10),dp(8));bar.setBackground(round(0xFF172536));
        TextView brand=label("▣  REI OS  ·  COMPUTADOR DA OFICINA",14,Color.WHITE);bar.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        TextView clock=label("● ONLINE",10,0xFF77E0A0);bar.addView(clock);desktop.addView(bar);
        TextView welcome=label("ÁREA DE TRABALHO",12,0xFFB8D4E9);welcome.setPadding(dp(4),dp(18),0,dp(10));desktop.addView(welcome);
        LinearLayout appTile=new LinearLayout(this);appTile.setOrientation(LinearLayout.VERTICAL);appTile.setGravity(Gravity.CENTER);appTile.setPadding(dp(18),dp(16),dp(18),dp(16));appTile.setBackground(round(0xCC2469A0));
        LinearLayout.LayoutParams tileParams=new LinearLayout.LayoutParams(dp(142),dp(132));tileParams.gravity=Gravity.LEFT;desktop.addView(appTile,tileParams);
        TextView appIcon=label("🛒",34,Color.WHITE);appIcon.setGravity(Gravity.CENTER);appTile.addView(appIcon);
        TextView appName=label("PEÇAS",16,Color.WHITE);appName.setGravity(Gravity.CENTER);appTile.addView(appName);
        TextView appDesc=label("Loja de componentes",10,0xFFD5E8F6);appDesc.setGravity(Gravity.CENTER);appTile.addView(appDesc);
        TextView tip=label("Toque no aplicativo PEÇAS para abrir a loja.",12,0xFFB8D4E9);tip.setPadding(0,dp(18),0,0);desktop.addView(tip);
        LinearLayout bottom=new LinearLayout(this);bottom.setGravity(Gravity.CENTER_VERTICAL);bottom.setPadding(dp(10),dp(9),dp(10),dp(9));bottom.setBackground(round(0xFF172536));
        TextView moneyText=label("CARTEIRA  ·  R$ "+money,13,0xFFFFD166);bottom.addView(moneyText,new LinearLayout.LayoutParams(0,-2,1));
        TextView closeText=label("FECHAR",11,Color.WHITE);bottom.addView(closeText);desktop.addView(new View(this),new LinearLayout.LayoutParams(1,0,1));desktop.addView(bottom);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("COMPUTADOR").setView(desktop).setNegativeButton("DESLIGAR",null).create();
        appTile.setOnClickListener(v->{appTile.animate().alpha(0.75f).scaleX(0.94f).scaleY(0.94f).setDuration(90).withEndAction(()->{dialog.dismiss();showPartsApp();}).start();});
        dialog.show();dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels*0.94),(int)(getResources().getDisplayMetrics().heightPixels*0.82));
        desktop.setAlpha(0f);desktop.setTranslationY(dp(18));desktop.animate().alpha(1f).translationY(0).setDuration(260).start();
    }
    void showPartsApp(){
        LinearLayout home=new LinearLayout(this);home.setOrientation(LinearLayout.VERTICAL);home.setPadding(dp(14),dp(12),dp(14),dp(12));home.setBackgroundColor(0xFF101923);
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.VERTICAL);top.setPadding(dp(12),dp(12),dp(12),dp(12));top.setBackground(round(0xFF1D3042));
        top.addView(label("▣  PEÇAS STORE",22,Color.WHITE));top.addView(label("Aplicativo da oficina · fornecedor de componentes",12,0xFF9DB9D5));
        TextView balance=label("SALDO: R$ "+money,14,0xFFFFD166);balance.setPadding(0,dp(8),0,0);top.addView(balance);home.addView(top);
        TextView hint=label("ESCOLHA UMA CATEGORIA",13,0xFF9DB9D5);hint.setPadding(0,dp(14),0,dp(8));home.addView(hint);
        String[] names={"COMPUTADORES / PCs","CELULARES","CONSOLES"};
        String[] desc={"Processadores, placas-mãe, RAM, GPU, SSD e muito mais","Telas, baterias, câmeras, conectores e componentes","Controles, analógicos, HDMI, leitores e refrigeração"};
        String[] icons={"▣","▯","◉"};
        int[] colors={0xFF245B8D,0xFF286C70,0xFF684A91};
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("COMPUTADOR · APLICATIVOS").setView(home).setNegativeButton("FECHAR",null).create();
        for(int k=0;k<3;k++){
            final int category=k;
            LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);tile.setPadding(dp(14),dp(13),dp(14),dp(13));tile.setBackground(round(colors[k]));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(10);home.addView(tile,p);
            tile.addView(label(icons[k]+"   "+names[k],17,Color.WHITE));TextView d=label(desc[k],11,0xFFE0EAF2);d.setPadding(0,dp(5),0,0);tile.addView(d);
            tile.setOnClickListener(v->{tile.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).withEndAction(()->{tile.animate().scaleX(1f).scaleY(1f).setDuration(100).start();showShopCategory(category);}).start();});
        }
        dialog.show();dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels*0.92),(int)(getResources().getDisplayMetrics().heightPixels*0.84));
    }
    void showShopCategory(int category){
        String title=category==0?"COMPUTADORES / PCs":category==1?"CELULARES":"CONSOLES";
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(12),dp(8),dp(12),dp(8));page.setBackgroundColor(0xFF111A25);
        TextView intro=label("PEÇAS STORE  /  "+title,13,0xFF9DB9D5);page.addView(intro);
        ScrollView scroll=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(0,dp(8),0,dp(8));scroll.addView(list);
        for(int i=0;i<parts.length;i++)if(partCategory[i]==category)addPartCard(list,i,true,null);
        page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView footer=label("CAIXA DISPONÍVEL: R$ "+money,14,0xFFFFD166);page.addView(footer);
        new AlertDialog.Builder(this).setTitle("PEÇAS STORE · "+title).setView(page).setPositiveButton("VOLTAR ÀS CATEGORIAS",(d,w)->showShop()).setNegativeButton("FECHAR",null).show();
    }
    void showParts(){showPartsCatalog();}
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
            // telefone e controle em manutenção sobre a bancada
            draw(0.95f,1.34f,0.62f,0.38f,0.07f,0.62f,0.035f,0.045f,0.06f);
            draw(0.95f,1.385f,0.62f,0.30f,0.025f,0.52f,0.12f,0.48f,0.76f);
            draw(2.85f,1.34f,0.12f,0.78f,0.10f,0.38f,0.09f,0.10f,0.12f);
            draw(2.58f,1.39f,0.12f,0.22f,0.08f,0.18f,0.16f,0.68f,0.42f);
            draw(3.12f,1.39f,0.12f,0.22f,0.08f,0.18f,0.16f,0.68f,0.42f);
            // cliente aguardando atendimento
            draw(4.55f,0.58f,-2.0f,0.20f,0.62f,0.22f,0.12f,0.14f,0.19f);
            draw(4.55f,1.28f,-2.0f,0.52f,0.68f,0.30f,0.16f,0.42f,0.72f);
            draw(4.55f,1.93f,-2.0f,0.31f,0.32f,0.31f,0.72f,0.50f,0.34f);
            draw(4.55f,2.10f,-2.0f,0.33f,0.12f,0.33f,0.10f,0.075f,0.055f);
            // caixas e aparelhos expostos na prateleira
            draw(-5.65f,3.55f,-3.7f,0.42f,0.65f,0.62f,0.12f,0.16f,0.22f);
            draw(-5.65f,3.58f,-3.35f,0.28f,0.48f,0.04f,0.10f,0.50f,0.82f);
            draw(-3.75f,3.55f,-3.7f,0.58f,0.35f,0.72f,0.08f,0.09f,0.11f);
            draw(-3.75f,3.78f,-3.7f,0.44f,0.10f,0.58f,0.16f,0.68f,0.42f);
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