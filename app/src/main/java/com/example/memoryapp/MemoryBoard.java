package com.example.memoryapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.JsonReader;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;

import javax.net.ssl.HttpsURLConnection;

import static com.example.memoryapp.Login.EXPIRATION;
import static com.example.memoryapp.Login.SHARED_PREFS;
import static com.example.memoryapp.Login.TOKEN;
import static java.lang.Integer.parseInt;
import static java.lang.Integer.valueOf;

public class MemoryBoard extends AppCompatActivity {

    ImageView[] imageViews = new ImageView[12];

    private TextView score;
    private TextView highscore;


    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String HIGHSCORE = "highscore";

    CountUpTimer scoreTimer;

    private boolean gameStarted = false;
    private boolean gameover = false;

    //Kaartjes in het spel
    int[] cardsArray = new int[]{0, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 5};

    //Daadwerkelijke plaatjes
    int[]images = new int[6];

    int firstCard, secondCard;
    int clickedFirst, clickedSecond;
    int cardNumber = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory);

        score = findViewById(R.id.score);
        highscore = findViewById(R.id.highscoreApp);

        //laad kaart plaatjes
        initFrondOfCardsResources();
        setImages();
        setOnClickListeners();
//        Collections.shuffle(Arrays.asList(cardsArray));
        Intent intent = getIntent();

        loadData();
    }



    public void logOut(){
        removeSavedData();

        Intent intent = new Intent(this, Login.class);
        startActivity(intent);
    }

    public void logOutPressed(View view) {
        logOut();
    }

    public void deleteHighscore(View view) { removeSavedHighscore(); }


    public void removeSavedHighscore(){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(HIGHSCORE);
        editor.apply();

        highscore.setText("1000");
    }

    public void removeSavedData(){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(TOKEN);
        editor.remove(EXPIRATION);
        editor.apply();
    }

    private void setImages() {
//        for (int i = 0; i < imageViews.length; i++) {
        imageViews[0] = findViewById(R.id.iv0);
        imageViews[1] = findViewById(R.id.iv1);
        imageViews[2] = findViewById(R.id.iv2);
        imageViews[3] = findViewById(R.id.iv3);
        imageViews[4] = findViewById(R.id.iv4);
        imageViews[5] = findViewById(R.id.iv5);
        imageViews[6] = findViewById(R.id.iv6);
        imageViews[7] = findViewById(R.id.iv7);
        imageViews[8] = findViewById(R.id.iv8);
        imageViews[9] = findViewById(R.id.iv9);
        imageViews[10] = findViewById(R.id.iv10);
        imageViews[11] = findViewById(R.id.iv11);

        imageViews[0].setTag("0");
        imageViews[1].setTag("1");
        imageViews[2].setTag("2");
        imageViews[3].setTag("3");
        imageViews[4].setTag("4");
        imageViews[5].setTag("5");
        imageViews[6].setTag("6");
        imageViews[7].setTag("7");
        imageViews[8].setTag("8");
        imageViews[9].setTag("9");
        imageViews[10].setTag("10");
        imageViews[11].setTag("11");

//            imageViews[i] = findViewById(parseInt("R.id.iv" + i));
//            imageViews[i].setTag(Integer.toString(i));
//        }
    }

    private void setOnClickListeners() {
        for(int i = 0; i < imageViews.length; i++) {
            final ImageView finalCurImageView = imageViews[i];
            imageViews[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int theCard = parseInt((String) v.getTag());
                    setImageOnClick(finalCurImageView, theCard);
                }
            });
        }
    }

    private void setImageOnClick(ImageView iv, int card) {
        //Draai kaartje om bij klik

        if(!gameStarted) {
            gameStarted = true;

            scoreTimer = new CountUpTimer(180000) {

                @Override
                public void onTick(int second) {
                    score.setText(String.valueOf(second));
                }

            };
            scoreTimer.start();
        }

        for (int i = 0; i < images.length; i++) {
            if (cardsArray[card] == i){
                iv.setImageResource(images[i]);
            }
        }

        //checken welk plaatje geselecteerd en opslaan in temperory
        if (cardNumber == 1) {
            firstCard = cardsArray[card];
            cardNumber = 2;
            clickedFirst = card;
            iv.setEnabled(false);
        } else if (cardNumber == 2) {
            secondCard = cardsArray[card];
            cardNumber = 1;
            clickedSecond = card;


            disableAllCards();;

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkIfEqual();
                }
            }, 500);
        }
    }

    public void updateScoreView(long score) {
        TextView textView = (TextView) findViewById(R.id.score);
        textView.setText(Long.toString(score));
    }


    private void disableAllCards() {
        for (int i = 0; i < imageViews.length; i++) {
            imageViews[i].setEnabled(false);
        }
    }

    private void enableAllCards() {
        for (int i = 0; i < imageViews.length; i++) {
            imageViews[i].setEnabled(true);
        }
    }

    private void checkIfEqual() {
        //als plaatjes gelijk zijn verwijder ze en check of spel voorbij is
        for (int i = 0; i < cardsArray.length; i++) {
            if (firstCard == secondCard) {
                if (clickedFirst == i) {
                    imageViews[i].setVisibility(View.INVISIBLE);
                }

                if (clickedSecond == i) {
                    imageViews[i].setVisibility(View.INVISIBLE);
                }
            } else {
                turnCardsToBack();
            }
        }
        enableAllCards();
        gameOverCheck();
    }

    private void turnCardsToBack(){
        for (int i = 0; i < imageViews.length; i++) {
            imageViews[i].setImageResource(R.drawable.back);
        }
    }

    private void gameOverCheck() {
        for (int i = 0; i < imageViews.length; i++) {
            if(imageViews[i].getVisibility() != View.INVISIBLE){
                return; //game is not over
            } else if (i == imageViews.length - 1){
                scoreTimer.cancel();

                int scoreInt = parseInt(score.getText().toString());
                int highscoreInt = parseInt(highscore.getText().toString());

                if( scoreInt < highscoreInt) {
                    highscore.setText(score.getText().toString());
                    save();
                }
//                serverRequest();
            }
//                serverRequest();
            }
        }


    private void updateWidget(){
        if(Integer.parseInt(highscore.getText().toString()) < Integer.parseInt(score.getText().toString())){
            highscore.setText(String.valueOf(score));
        }
    }


    private void initFrondOfCardsResources() {
        images[0] = R.drawable.image1;
        images[1] = R.drawable.image2;
        images[2] = R.drawable.image3;
        images[3] = R.drawable.image4;
        images[4] = R.drawable.image5;
        images[5] = R.drawable.image6;
    }


    public void save() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(HIGHSCORE, highscore.getText().toString());

        editor.apply();
        Toast.makeText(this, "Highscore saved!", Toast.LENGTH_SHORT).show();
    }

    public void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        String highscoreString = sharedPreferences.getString(HIGHSCORE, "1000");
        highscore.setText(highscoreString);
    }

    public void serverRequest() {
        try {
            URL url = new URL("http://10.0.2.2:5000/api/getscore");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(20000);
            urlConnection.setConnectTimeout(20000);
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            OutputStream os = urlConnection.getOutputStream();


            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write("");
            writer.flush();
            writer.close();
            os.close();

            int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                JsonReader jsonReader = new JsonReader(in);

                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    highscore.setText(jsonReader.nextString());
                }
                jsonReader.endObject();
                in.close();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
