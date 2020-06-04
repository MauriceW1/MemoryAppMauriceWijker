package com.example.memoryapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import static java.lang.Integer.parseInt;

public class Login extends AppCompatActivity {
    private String token = null;
    private int expiration = 0;
    private boolean tokenExpired = true;

    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String TOKEN = "SavedToken";
    public static final String EXPIRATION = "savedExpiration";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loadData();
        checkForTokenExpiration();

        if(!tokenExpired) {
            goToGame();
        }
    }


    public void loginButtonPressed(View view) {
        if(token == null || tokenExpired){
            authenticate(view);

            save();

        } else {
            goToGame();
        }
    }

    public void checkForTokenExpiration(){
        Thread tokenExpiredThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while ((System.currentTimeMillis() / 1000) < expiration){
                    tokenExpired = false;
                }
                logOut();
            }
        });

        if(expiration != 0){
            tokenExpiredThread.start();
        }
    }

    public void logOut(){
        removeSavedData();
        Intent intent = new Intent(this, Login.class);
        startActivity(intent);
    }

    public void authenticate(View view){
     Runnable run=()->{

            TextView username = findViewById(R.id.username);
            TextView password = findViewById(R.id.password);

            try {
                URL url = new URL("http://10.0.2.2:5000/api/login");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setReadTimeout(20000);
                urlConnection.setConnectTimeout(20000);
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);
                OutputStream os = urlConnection.getOutputStream();


                BufferedWriter writer = new BufferedWriter( new OutputStreamWriter(os, "UTF-8"));
                writer.write("name=" + username.getText().toString() + "&password=" + password.getText().toString());
                writer.flush();
                writer.close();
                os.close();

                int responseCode= urlConnection.getResponseCode();
                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader( new InputStreamReader(urlConnection.getInputStream()));
                    JsonReader jsonReader=new JsonReader(in);

                    jsonReader.beginObject();
                    while (jsonReader.hasNext()){
                        String name = jsonReader.nextName();
                        if(name.equals("token")){
                            token=jsonReader.nextString();
                            tokenExpired = false;
                            Log.d("token", token);
                        }else if(name.equals("expiresIn")){
                            expiration = jsonReader.nextInt();
                            Log.d("expiration", Integer.toString(expiration));
                        }
                        else {
                            jsonReader.skipValue();
                        }
                    }
                    jsonReader.endObject();
                    in.close();
                    goToGame();
                }


            } catch (UnsupportedEncodingException ex) {
                ex.printStackTrace();
            } catch (ProtocolException ex) {
                ex.printStackTrace();
            } catch (MalformedURLException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        };
        new Thread(run).start();
    }

    public void goToGame(){
        Intent intent = new Intent(this, MemoryBoard.class);
        intent.putExtra("expiration", expiration);
        intent.putExtra("SHARED_PREFS", SHARED_PREFS);
        startActivity(intent);
    }

    public void save() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(TOKEN, token);
        editor.putInt(EXPIRATION, expiration);

        editor.apply();
        Toast.makeText(this, "TOKEN & EXPIRATIONTIME SAVED", Toast.LENGTH_SHORT).show();
    }

    public void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        token = sharedPreferences.getString(TOKEN, "");
        expiration = sharedPreferences.getInt(EXPIRATION, 0);
    }

    public void removeSavedData(){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(TOKEN);
        editor.remove(EXPIRATION);
        editor.apply();
    }
}

