package com.thk.aktienkursrechner;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener{

    private String apiURL="https://www.alphavantage.co/query";

    private ProgressBar progressBar;
    private EditText symbol, zeit;
    private String symbolS;
    private int zeitI;
    private String einheit;

    private Spinner spinner;
    List<Aktie> aktieList;
    private TextView ausgabe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.progressBar);
        spinner = (Spinner) findViewById(R.id.spinner);
        symbol = (EditText) findViewById(R.id.symbol);
        zeit = (EditText) findViewById(R.id.zeit);
        ausgabe = (TextView) findViewById(R.id.ausgabe);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,R.array.time_array,android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
        einheit = spinner.getItemAtPosition(0).toString();

        progressBar.setVisibility(View.INVISIBLE);
    }

    public void rechne(View view) {
        ausgabe.setTextColor(Color.WHITE);

        apiURL="https://www.alphavantage.co/query";
        ausgabe.setText("");
        aktieList = new ArrayList<>();

        symbolS = symbol.getEditableText().toString();

        if(symbolS.equals("")){
            ausgabe.setText("Bitte geben Sie Tickersymbol und Zeitpunkt ein");
            ausgabe.setTextColor(Color.RED);
            return;
        }
        try {
            zeitI = Integer.parseInt(zeit.getEditableText().toString());
        }
        catch (Exception e){
            ausgabe.setText("Bitte überprüfen Sie die angegebene Zeit");

            ausgabe.setTextColor(Color.RED);
            return;
        }




        String time="";

        if(einheit.equals("Tag/e")){
            time="function=TIME_SERIES_DAILY";
        }else if(einheit.equals("Woche/n")){
            time = "function=TIME_SERIES_WEEKLY";
        }else if(einheit.equals("Monat/e")){
            time="function=TIME_SERIES_MONTHLY";
        }

        apiURL += "?"+time+"&symbol="+symbolS+"&apikey=Y2LQAW31V2HXBEJL";

        GetApiData getApiData = new GetApiData();
        getApiData.execute();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        this.einheit = parent.getItemAtPosition(position).toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }


    private class GetApiData extends AsyncTask<Void,Void,Void>{

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                URL myurl = new URL(apiURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection)myurl.openConnection();
                httpURLConnection.setDoInput(true);
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.connect();


                InputStream is = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
                String line ="";

                StringBuilder stringBuilder = new StringBuilder();
                while ((line = bufferedReader.readLine())!=null){
                    stringBuilder.append(line);
                }
                String data = stringBuilder.toString();



                JSONObject parentObject = new JSONObject(data);
                JSONObject time = null;
                Iterator<String> keyTimes = parentObject.keys();
                while (keyTimes.hasNext()){
                    String keyT = keyTimes.next();
                    if(keyT.equals("Meta Data")){
                        keyT = keyTimes.next();
                    }
                    time = parentObject.getJSONObject(keyT);
                }



                Iterator<String> keys = time.keys();
                while (keys.hasNext()){
                    Aktie aktie = new Aktie();
                    String key = keys.next();
                    JSONObject obj = time.getJSONObject(key);
                    aktie.setDatum(key);
                    aktie.setClose(obj.getDouble("4. close"));
                    aktie.setOpen(obj.getDouble("1. open"));
                    double rendite = Math.log(aktie.getClose()/aktie.getOpen());
                    aktie.setRendite(rendite);
                    aktieList.add(aktie);
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            progressBar.setVisibility(View.INVISIBLE);
            String out="";
            int n=0;
            double mittelwert;
            double rendite = 0;
            for(Aktie aktie: aktieList){
                n++;
                rendite += aktie.getRendite();
            }
            if(n>0){
                mittelwert = Math.round(rendite/n *1000000.0) / 1000000.0;

                double zaehler = 0;
                for (Aktie aktie: aktieList){
                    zaehler += Math.pow((aktie.getRendite()-mittelwert),2);
                }

                double sa = Math.round(Math.sqrt(zaehler/n) *1000000.0) / 1000000.0;

                double [] werte = new double[zeitI+1];
                double [] werteLast = new double[zeitI+1];

                werteLast[0] = aktieList.get(0).getClose();

                int c=0;

                while(c<zeitI) {

                    for (int k = 0; k < c + 2; k++) {
                        if (k == 0) {
                            werte[k] = berechneAufstieg(werteLast[k], mittelwert, sa);
                        } else {
                            werte[k] = berechneAbstieg(werteLast[k - 1], mittelwert, sa);
                        }
                    }
                    for (int k = 0; k < c + 2; k++) {
                        werteLast[k] = werte[k];
                    }
                    c++;
                }

                for(int i=0; i<werte.length; i++){
                    werte[i] = Math.round(werte[i] * 100.0) / 100.0;
                }

                //Date currentTime = Calendar.getInstance().getTime();
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                String date = sdf.format(new Date());

                SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
                String dt = sdf1.format(new Date());

                Calendar c1 = Calendar.getInstance();
                try {
                    c1.setTime(sdf1.parse(dt));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                if(einheit.equals("Tag/e")){
                    c1.add(Calendar.DATE, zeitI);
                }else if(einheit.equals("Woche/n")){
                    c1.add(Calendar.WEEK_OF_YEAR, zeitI);
                }
                else if(einheit.equals("Monat/e")){
                    c1.add(Calendar.MONTH, zeitI);
                }
                dt = sdf.format(c1.getTime());



                double akt = Math.round(aktieList.get(0).getClose() * 100.0) /100.0;

                out = "Aktueller Wert("+date+"): "+akt+"$\n\n";
                out += "Prognostzitierte Werte am "+dt+"\n";
                for(int i = 0; i<werte.length;i++){
                    out +=werte[i]+"$\n";
                }
                //out="mittelwert: "+mittelwert+" SA: "+sa+"\nAktuller Wert: "+akt+"$\n1. Aufstieg: "+aufstieg+"$\n1.Abstieg: "+abstieg+"$";
            }

            if(aktieList.isEmpty()) out = "Bitte Internetverbindung oder Tickersymbol überprüfen!";
            ausgabe.setText(out);
        }

        private double berechneAufstieg(double akt,double mittelwert, double sa){
            return Math.pow(Math.E,mittelwert+sa)*akt;
        }

        private double berechneAbstieg(double akt,double mittelwert, double sa){
            return Math.pow(Math.E,mittelwert-sa)*akt;
        }
    }
}
