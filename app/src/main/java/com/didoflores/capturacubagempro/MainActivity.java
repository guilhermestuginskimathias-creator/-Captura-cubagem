package com.didoflores.capturacubagempro;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import android.graphics.pdf.PdfDocument;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import org.json.*;

public class MainActivity extends Activity {
    private static final int REQ_PRODUCT_PHOTO = 20;
    private static final int REQ_CARD_PHOTO = 21;
    private static final int REQ_CREATE_PDF = 30;
    private static final double CONTAINER_MAX_M3 = 68.0;

    private final ArrayList<Item> items = new ArrayList<>();
    private LinearLayout listBox;
    private TextView statItems, statVolume, statPercent;
    private Bitmap pendingProductPhoto, pendingCardPhoto;
    private Item editingItem;
    private final DecimalFormat df3 = new DecimalFormat("0.000");
    private final DecimalFormat df2 = new DecimalFormat("0.00");

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        loadItems();
        showHome();
    }

    private TextView tv(String text, float sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(text); v.setTextSize(sp); v.setTextColor(color);
        v.setPadding(dp(4), dp(4), dp(4), dp(4));
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private GradientDrawable bg(int color, float radius) {
        GradientDrawable g = new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp((int)radius)); return g;
    }

    private Button button(String text, int color) {
        Button b = new Button(this); b.setText(text); b.setTextColor(Color.WHITE); b.setTextSize(16); b.setAllCaps(false);
        b.setBackground(bg(color, 18)); b.setPadding(dp(14), dp(12), dp(14), dp(12));
        return b;
    }

    private EditText input(String hint, int type) {
        EditText e = new EditText(this); e.setHint(hint); e.setTextSize(16); e.setInputType(type); e.setSingleLine(true);
        e.setPadding(dp(14), dp(12), dp(14), dp(12)); e.setBackground(bg(0xFFF3F5F7, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, dp(6), 0, dp(6)); e.setLayoutParams(lp); return e;
    }

    private void showHome() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(0xFFF7F8FA);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18), dp(18), dp(18), dp(30)); scroll.addView(root);

        LinearLayout hero = new LinearLayout(this); hero.setOrientation(LinearLayout.VERTICAL); hero.setPadding(dp(18), dp(18), dp(18), dp(18)); hero.setBackground(bg(0xFFFF6B57, 24));
        hero.addView(tv("Captura & Cubagem Pro", 26, Color.WHITE, true));
        hero.addView(tv("Catálogo, cubagem do container e PDF em um só lugar", 15, 0xFFFFEFEA, false));
        root.addView(hero, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout stats = new LinearLayout(this); stats.setOrientation(LinearLayout.HORIZONTAL); stats.setPadding(0, dp(14), 0, dp(14));
        statItems = statCard(stats, "Itens", "0"); statVolume = statCard(stats, "Cubagem", "0,000 m³"); statPercent = statCard(stats, "Container", "0%");
        root.addView(stats);

        TextView title = tv("Container atual", 20, 0xFF20242A, true); root.addView(title);
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal); bar.setMax(100); bar.setProgress((int)Math.round(totalVolume()/CONTAINER_MAX_M3*100));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, dp(16)); blp.setMargins(0, dp(8), 0, dp(4)); root.addView(bar, blp);
        root.addView(tv(df3.format(totalVolume()) + " / 68,000 m³", 15, 0xFF60656B, false));

        Button add = button("＋ Fotografar / cadastrar produto", 0xFFFF6B57); add.setOnClickListener(v -> openItemForm(null));
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(-1, -2); alp.setMargins(0, dp(18), 0, dp(8)); root.addView(add, alp);

        Button pdf = button("Gerar catálogo em PDF", 0xFF176B5B); pdf.setOnClickListener(v -> createPdfPicker());
        root.addView(pdf, new LinearLayout.LayoutParams(-1, -2));

        root.addView(tv("Produtos", 22, 0xFF20242A, true), marginTop(dp(22)));
        listBox = new LinearLayout(this); listBox.setOrientation(LinearLayout.VERTICAL); root.addView(listBox);
        refreshHome();
        setContentView(scroll);
    }

    private TextView statCard(LinearLayout parent, String label, String value) {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(12), dp(12), dp(12), dp(12)); card.setBackground(bg(Color.WHITE, 18));
        TextView val = tv(value, 18, 0xFF20242A, true); card.addView(val); card.addView(tv(label, 12, 0xFF777B82, false));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f); lp.setMargins(dp(3), 0, dp(3), 0); parent.addView(card, lp); return val;
    }

    private LinearLayout.LayoutParams marginTop(int px) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,px,0,0); return p; }

    private void refreshHome() {
        if (statItems == null) return;
        double total = totalVolume(); int pct = (int)Math.min(100, Math.round(total/CONTAINER_MAX_M3*100));
        statItems.setText(String.valueOf(items.size())); statVolume.setText(df3.format(total) + " m³"); statPercent.setText(pct + "%");
        listBox.removeAllViews();
        if (items.isEmpty()) { listBox.addView(tv("Nenhum produto cadastrado ainda.", 15, 0xFF777B82, false)); return; }
        for (Item it : items) listBox.addView(itemCard(it), marginTop(dp(10)));
    }

    private View itemCard(Item it) {
        LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(14), dp(14), dp(14), dp(14)); c.setBackground(bg(Color.WHITE, 20));
        if (it.photo != null) { ImageView im = new ImageView(this); im.setImageBitmap(it.photo); im.setScaleType(ImageView.ScaleType.CENTER_CROP); c.addView(im, new LinearLayout.LayoutParams(-1, dp(170))); }
        c.addView(tv(it.description.isEmpty()?"Produto sem descrição":it.description, 19, 0xFF20242A, true));
        c.addView(tv((it.code.isEmpty()?"Sem código":it.code) + " • " + (it.supplier.isEmpty()?"Sem fornecedor":it.supplier), 13, 0xFF777B82, false));
        c.addView(tv("Qtd. " + it.qty + " • " + df2.format(it.length) + " × " + df2.format(it.width) + " × " + df2.format(it.height) + " cm", 14, 0xFF555A60, false));
        c.addView(tv("Cubagem total: " + df3.format(it.volume()) + " m³", 16, 0xFF176B5B, true));
        if (it.price > 0) c.addView(tv("Preço unitário: " + df2.format(it.price), 14, 0xFF555A60, false));
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setPadding(0,dp(8),0,0);
        Button edit = button("Editar", 0xFF5B626A); edit.setOnClickListener(v -> openItemForm(it)); row.addView(edit, new LinearLayout.LayoutParams(0,-2,1));
        Button del = button("Excluir", 0xFFC94A4A); del.setOnClickListener(v -> { items.remove(it); saveItems(); showHome(); }); LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(0,-2,1); dlp.setMargins(dp(8),0,0,0); row.addView(del, dlp); c.addView(row);
        return c;
    }

    private void openItemForm(Item current) {
        editingItem = current; pendingProductPhoto = current == null ? null : current.photo; pendingCardPhoto = current == null ? null : current.cardPhoto;
        ScrollView s = new ScrollView(this); LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.VERTICAL); r.setPadding(dp(18),dp(18),dp(18),dp(30)); s.addView(r); r.setBackgroundColor(0xFFF7F8FA);
        r.addView(tv(current==null?"Novo produto":"Editar produto", 26, 0xFF20242A, true));
        final ImageView preview = new ImageView(this); preview.setScaleType(ImageView.ScaleType.CENTER_CROP); preview.setBackground(bg(0xFFE9EDF0,18)); if (pendingProductPhoto != null) preview.setImageBitmap(pendingProductPhoto); r.addView(preview, marginTop(dp(12))); preview.getLayoutParams().height=dp(200);
        Button take = button("📷 Tirar foto do produto", 0xFFFF6B57); take.setOnClickListener(v -> startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), REQ_PRODUCT_PHOTO)); r.addView(take, marginTop(dp(10)));
        Button card = button("💳 Fotografar cartão do fornecedor", 0xFF5B626A); card.setOnClickListener(v -> startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), REQ_CARD_PHOTO)); r.addView(card, marginTop(dp(8)));

        EditText desc=input("Descrição do produto",1), code=input("Código",1), supplier=input("Fornecedor",1), phone=input("Telefone / WeChat",1);
        EditText price=input("Preço unitário",8194), qty=input("Quantidade",2), len=input("Comprimento (cm)",8194), wid=input("Largura (cm)",8194), hei=input("Altura (cm)",8194), weight=input("Peso unitário (kg) - opcional",8194);
        r.addView(desc); r.addView(code); r.addView(supplier); r.addView(phone); r.addView(price); r.addView(qty); r.addView(len); r.addView(wid); r.addView(hei); r.addView(weight);
        if (current != null) {
            desc.setText(current.description); code.setText(current.code); supplier.setText(current.supplier); phone.setText(current.phone);
            price.setText(String.valueOf(current.price)); qty.setText(String.valueOf(current.qty)); len.setText(String.valueOf(current.length)); wid.setText(String.valueOf(current.width)); hei.setText(String.valueOf(current.height)); weight.setText(String.valueOf(current.weight));
        }
        Button save = button("Salvar produto", 0xFF176B5B); r.addView(save, marginTop(dp(14)));
        save.setOnClickListener(v -> {
            Item it = current == null ? new Item() : current;
            it.description=desc.getText().toString().trim(); it.code=code.getText().toString().trim(); it.supplier=supplier.getText().toString().trim(); it.phone=phone.getText().toString().trim();
            it.price=num(price); it.qty=(int)Math.max(1,num(qty)); it.length=num(len); it.width=num(wid); it.height=num(hei); it.weight=num(weight); it.photo=pendingProductPhoto; it.cardPhoto=pendingCardPhoto;
            if (it.length<=0 || it.width<=0 || it.height<=0) { Toast.makeText(this,"Informe comprimento, largura e altura.",Toast.LENGTH_LONG).show(); return; }
            if (current == null) items.add(it); saveItems(); showHome();
        });
        Button cancel=button("Voltar",0xFF8A8F95); cancel.setOnClickListener(v->showHome()); r.addView(cancel, marginTop(dp(8))); setContentView(s);
    }

    private double num(EditText e) { try { return Double.parseDouble(e.getText().toString().replace(',','.')); } catch(Exception x) { return 0; } }

    @Override protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req,result,data); if (result != RESULT_OK) return;
        if (req==REQ_PRODUCT_PHOTO || req==REQ_CARD_PHOTO) {
            Bitmap b = data==null?null:(Bitmap)data.getExtras().get("data");
            if (req==REQ_PRODUCT_PHOTO) pendingProductPhoto=b; else pendingCardPhoto=b;
            Toast.makeText(this, req==REQ_PRODUCT_PHOTO?"Foto do produto capturada":"Cartão do fornecedor capturado",Toast.LENGTH_SHORT).show();
            openItemForm(editingItem);
        } else if (req==REQ_CREATE_PDF && data!=null) {
            Uri uri=data.getData(); if(uri!=null) writePdf(uri);
        }
    }

    private void createPdfPicker() {
        if(items.isEmpty()){ Toast.makeText(this,"Cadastre pelo menos um produto.",Toast.LENGTH_SHORT).show(); return; }
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT); i.setType("application/pdf"); i.putExtra(Intent.EXTRA_TITLE,"catalogo_cubagem.pdf"); startActivityForResult(i,REQ_CREATE_PDF);
    }

    private void writePdf(Uri uri) {
        PdfDocument pdf=new PdfDocument(); int pageNo=1; int y=60; PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo).create()); Canvas c=page.getCanvas(); Paint p=new Paint(1);
        p.setColor(0xFFFF6B57); c.drawRect(0,0,595,90,p); p.setColor(Color.WHITE); p.setTextSize(25); p.setTypeface(Typeface.DEFAULT_BOLD); c.drawText("Captura & Cubagem Pro",28,43,p); p.setTextSize(12); p.setTypeface(Typeface.DEFAULT); c.drawText("Catálogo e relatório de cubagem",28,67,p); y=125;
        p.setColor(0xFF20242A); p.setTextSize(15); p.setTypeface(Typeface.DEFAULT_BOLD); c.drawText("Resumo do container",28,y,p); y+=24; p.setTypeface(Typeface.DEFAULT); c.drawText("Itens: "+items.size()+"   Cubagem: "+df3.format(totalVolume())+" m³ / 68,000 m³",28,y,p); y+=35;
        for(Item it:items){
            if(y>720){ pdf.finishPage(page); pageNo++; page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo).create()); c=page.getCanvas(); y=55; }
            if(it.photo!=null){ Bitmap bm=Bitmap.createScaledBitmap(it.photo,120,95,true); c.drawBitmap(bm,28,y,p); }
            int tx=it.photo!=null?165:28; p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(16); p.setColor(0xFF20242A); c.drawText(safe(it.description,"Produto"),tx,y+18,p);
            p.setTypeface(Typeface.DEFAULT); p.setTextSize(11); c.drawText("Código: "+safe(it.code,"-")+"  Fornecedor: "+safe(it.supplier,"-"),tx,y+39,p);
            c.drawText("Qtd: "+it.qty+"  Medidas: "+df2.format(it.length)+" x "+df2.format(it.width)+" x "+df2.format(it.height)+" cm",tx,y+58,p);
            c.drawText("Preço: "+df2.format(it.price)+"   Peso: "+df2.format(it.weight)+" kg",tx,y+77,p);
            p.setTypeface(Typeface.DEFAULT_BOLD); p.setColor(0xFF176B5B); c.drawText("Cubagem: "+df3.format(it.volume())+" m³",tx,y+96,p);
            y+=125; p.setColor(0xFFE0E3E6); c.drawLine(28,y-10,567,y-10,p); p.setColor(0xFF20242A);
        }
        pdf.finishPage(page);
        try(OutputStream os=getContentResolver().openOutputStream(uri)){ pdf.writeTo(os); Toast.makeText(this,"PDF gerado com sucesso.",Toast.LENGTH_LONG).show(); }
        catch(Exception e){ Toast.makeText(this,"Erro ao gerar PDF: "+e.getMessage(),Toast.LENGTH_LONG).show(); }
        finally{ pdf.close(); }
    }

    private String safe(String s,String d){ return s==null||s.trim().isEmpty()?d:s; }
    private double totalVolume(){ double t=0; for(Item i:items)t+=i.volume(); return t; }
    private int dp(int v){ return (int)(v*getResources().getDisplayMetrics().density+0.5f); }

    private void saveItems(){
        JSONArray a=new JSONArray(); try{ for(Item i:items)a.put(i.json()); getSharedPreferences("db",0).edit().putString("items",a.toString()).apply(); }catch(Exception ignored){}
    }
    private void loadItems(){
        String s=getSharedPreferences("db",0).getString("items","[]"); try{ JSONArray a=new JSONArray(s); for(int n=0;n<a.length();n++) items.add(Item.from(a.getJSONObject(n))); }catch(Exception ignored){}
    }

    static class Item {
        String description="",code="",supplier="",phone=""; double price=0,length=0,width=0,height=0,weight=0; int qty=1; Bitmap photo,cardPhoto;
        double volume(){ return (length*width*height/1_000_000d)*qty; }
        JSONObject json() throws Exception { JSONObject o=new JSONObject(); o.put("description",description);o.put("code",code);o.put("supplier",supplier);o.put("phone",phone);o.put("price",price);o.put("length",length);o.put("width",width);o.put("height",height);o.put("weight",weight);o.put("qty",qty);o.put("photo",enc(photo));o.put("card",enc(cardPhoto));return o; }
        static Item from(JSONObject o)throws Exception{ Item i=new Item();i.description=o.optString("description");i.code=o.optString("code");i.supplier=o.optString("supplier");i.phone=o.optString("phone");i.price=o.optDouble("price");i.length=o.optDouble("length");i.width=o.optDouble("width");i.height=o.optDouble("height");i.weight=o.optDouble("weight");i.qty=o.optInt("qty",1);i.photo=dec(o.optString("photo"));i.cardPhoto=dec(o.optString("card"));return i; }
        static String enc(Bitmap b){ if(b==null)return ""; ByteArrayOutputStream x=new ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.JPEG,70,x);return android.util.Base64.encodeToString(x.toByteArray(),android.util.Base64.NO_WRAP); }
        static Bitmap dec(String s){ try{ if(s==null||s.isEmpty())return null;byte[] b=android.util.Base64.decode(s,android.util.Base64.DEFAULT);return BitmapFactory.decodeByteArray(b,0,b.length);}catch(Exception e){return null;} }
    }
}
