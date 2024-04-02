package com.example.get_request;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.DialogFragment;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;



public class MainActivity extends AppCompatActivity {
    private TextView textViewResult;

    //Initialize variables of picker
    TextView tvTimer1,tvTimer2,tvTimer1text,tvTimer2text;
    int t1Hour,t1Minute,t2Hour,t2Minute;


    //handler implementation:
    Handler handler = new Handler();
    int apiDelayed = 1*1000; //1 second=1000 milisecond
    Runnable runnable;

    boolean already_open=false;
    String open_at="";
    int hour=0;
    int minute=0;
    int counter=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewResult=findViewById(R.id.text_view_result);

        getData();

        //assign variable
        tvTimer1 = findViewById(R.id.button1);
        tvTimer2 = findViewById(R.id.button2);

        tvTimer1text=findViewById(R.id.textView1);
        tvTimer2text=findViewById(R.id.textView2);

         tvTimer1.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 //initialize time picker dialog
                 TimePickerDialog timePickerDialog = new TimePickerDialog(
                         MainActivity.this,
                         new TimePickerDialog.OnTimeSetListener() {
                             @Override
                             public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                 //Initialize hour and minute
                                 t1Hour= hourOfDay;
                                 t1Minute=minute;
                                 //initialize calendar
                                 Calendar calendar=Calendar.getInstance();

                                 calendar.set(0,0,0,t1Hour,t1Minute);

                                 tvTimer1text.setText(DateFormat.format("hh:mm aa",calendar));
                             }
                         },12,0,false
                 );
                 //Displayed previous selected time
                 timePickerDialog.updateTime(t1Hour,t1Minute);
                 //show dialog
                 timePickerDialog.show();
             }
         });

        tvTimer2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //initialize time picker dialog
                TimePickerDialog timePickerDialog2 = new TimePickerDialog(
                        MainActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                //Initialize hour and minute
                                t2Hour= hourOfDay;
                                t2Minute=minute;
                                //initialize calendar
                                Calendar calendar=Calendar.getInstance();

                                calendar.set(0,0,0,t2Hour,t2Minute);

                                tvTimer2text.setText(DateFormat.format("hh:mm aa",calendar));
                            }
                        },12,0,false
                );
                //Displayed previous selected time
                timePickerDialog2.updateTime(t2Hour,t2Minute);
                //show dialog
                timePickerDialog2.show();
            }
        });

    }
    @Override
    protected void onResume() {
        super.onResume();

        handler.postDelayed( runnable = new Runnable() {
            public void run() {
                //do your function;
                getData();
                handler.postDelayed(runnable, apiDelayed);
            }
        }, apiDelayed); // so basically after your getHeroes(), from next time it will be 5 sec repeated
    }
    @Override
    protected void onPause() {
        super.onPause();
        //handler.removeCallbacks(runnable); //stop handler when activity not visible
        handler.postDelayed(runnable, apiDelayed);//continue working
    }
    public void getData() {
        Date currentTime = Calendar.getInstance().getTime();
        String test=DateFormat.format("HH:mm:ss ",currentTime).toString();
        int time_in_minutes=Integer.parseInt(test.substring(0,2))*60+Integer.parseInt(test.substring(3,5));
        int seconds= Integer.parseInt(test.substring(6,8));


        Retrofit retrofit= new Retrofit.Builder()
                .baseUrl("https://api.thingspeak.com/channels/1325488/fields/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        JsonPlaceHolderApi jsonPlaceHolderApi= retrofit.create(JsonPlaceHolderApi.class);
        Call<Post> call = jsonPlaceHolderApi.getPosts();

        call.enqueue(new Callback<Post>() {
            @Override
            public void onResponse(Call<Post> call, Response<Post> response) {
                if(!response.isSuccessful()){
                    textViewResult.setText("Code"+response.code());
                    return;
                }
                Post posts = response.body();
                List<feed> feeds = posts.getFeeds();
                for (feed feed: feeds){
                    String content="";
                    int state= Integer.parseInt(feed.getField1());


                    if (state==0){//porte fermée
                        content="La porte est fermée";
                        already_open=false;
                        open_at="";
                        counter=0;
                    }
                    else{//porte ouverte
                        counter=time_in_minutes-(hour*60+minute);//in seconds as each loop take one second
                        content="La porte est ouverte "+" depuis: "+ open_at;


                        if (!already_open){//first signal that shows that the door is open.
                            int time= Integer.parseInt(feed.getCreated_at().substring(11,13))+2;
                            open_at=Integer.toString(time%24)+feed.getCreated_at().substring(13,19);//11 to 19 but modifying time
                            hour=Integer.parseInt(open_at.substring(0,2));
                            minute=Integer.parseInt(open_at.substring(3,5));

                            if (!(t1Hour*60+t1Minute<time_in_minutes && time_in_minutes<t2Hour*60+t2Minute)){//notify just when is necessary
                            getNotification(open_at);
                            }
                        }
                        // is not the first signal
                        else if(counter%30==0 && seconds==0 && !(t1Hour*60+t1Minute<hour*60+minute && hour*60+minute<t2Hour*60+t2Minute)){//each 1minute (to test) or desired time we want to send a notification
                            getNotification(open_at);
                        }
                        already_open=true;
                    }
                    textViewResult.setText(content);
                }
            }

            @Override
            public void onFailure(Call<Post> call, Throwable t) {
                textViewResult.setText(t.getMessage());
            }


        });
    }


    public void getNotification(String time) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "my_channel_id_01";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications",NotificationManager.IMPORTANCE_HIGH);

            // Configure the notification channel.
            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }


        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);

        notificationBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setTicker("Hearty365")
                //     .setPriority(Notification.PRIORITY_MAX)
                .setContentTitle("La Porte est ouverte")
                .setContentText("Votre porte est ouverte depuis:"+ time)
                .setContentInfo("Info");

        notificationManager.notify(/*notification id*/1, notificationBuilder.build());
    }
}