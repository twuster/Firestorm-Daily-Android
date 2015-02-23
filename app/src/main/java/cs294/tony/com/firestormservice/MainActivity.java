package cs294.tony.com.firestormservice;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;


public class MainActivity extends ActionBarActivity {

    private TextView mWeatherText;
    private TextView mMessageText;
    private ImageView mWeatherIcon;

    private static enum TempUnit {FAHRENHEIT, CELSIUS};
    private static TempUnit currentUnit = TempUnit.CELSIUS;
    private static final String TAG = "Firestorm";

    private NotificationManager mNM;
    private int NOTIFICATION = R.string.local_service_started;
    private DatagramSocket serverSocket;
    private Thread serverThread;
    private static final int PORT_NUM = 1527;
    private String message;
    byte[] buffer = new byte[256];
    private String mCurrentTemp;
    DatagramPacket pckt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        if (savedInstanceState == null) {
//            getSupportFragmentManager().beginTransaction()
//                    .add(R.id.container, new PlaceholderFragment())
//                    .commit();
//        }
        mWeatherText = (TextView) findViewById(R.id.temperature_text);
        mMessageText = (TextView) findViewById(R.id.message);
        mWeatherIcon = (ImageView) findViewById(R.id.weather_image);
        mCurrentTemp = "Unknown";



        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification();
        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();
    }

    @Override
    public void onStop(){
        super.onStop();
        mNM.cancel(NOTIFICATION);
        this.serverThread.interrupt();
        serverSocket.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_change_unit:
                changeUnit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void changeUnit(){
        int temp_int;
        int new_temp;
        if (!mCurrentTemp.equals("Unknown")){
            temp_int = Integer.parseInt(mCurrentTemp);

            if (currentUnit == TempUnit.CELSIUS){
                currentUnit = TempUnit.FAHRENHEIT;
                new_temp = ctoF(temp_int);
                setWeatherText(Integer.toString(new_temp));
            } else {
                currentUnit = TempUnit.CELSIUS;
                new_temp = ftoC(temp_int);
                setWeatherText(Integer.toString(new_temp));
            }
        }
    }

    class ServerThread implements Runnable {

        public void run() {
            try {
                serverSocket = new DatagramSocket(PORT_NUM);

            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!Thread.currentThread().isInterrupted()) {

                try {

                    pckt = new DatagramPacket(buffer, buffer.length);
                    pckt.setLength(buffer.length);

                    serverSocket.receive(pckt);
                    message = stringFromPacket(pckt);
                    final String[] split = message.split(":");
                    final String temp = split[0];
                    final String tidbit = split[1];
                    Log.e(TAG, message);
                    MainActivity.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if (currentUnit == TempUnit.CELSIUS) {
                                setWeatherText(temp);
                            } else {
                                setWeatherText(Integer.toString(ctoF(Integer.parseInt(temp))));
                            }
                            changeBackgroundAndIcon(temp);
                            showMessage(tidbit);
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Log.e(TAG, "Broken out");

        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

    public void setWeatherText(String temp) {
        mCurrentTemp = temp;
        if (currentUnit == TempUnit.CELSIUS) {
            mWeatherText.setText(temp + (char) 0x00B0 + " Celsius");
        } else {
            mWeatherText.setText(temp + (char) 0x00B0 + " Fahrenheit");
        }
    }

    private int ftoC(int temp) {
        temp = ((temp - 32)*5)/9;
        return temp;
    }

    private int ctoF(int temp) {
        temp = 9*(temp/5) + 32;
        return temp;
    }

    public void changeBackgroundAndIcon(String temp) {
        int temperature = Integer.parseInt(temp);
        View base = findViewById(R.id.container);
        if (temperature > 30) {
            mWeatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.clear));
            base.setBackgroundColor(getResources().getColor(R.color.light_red));
        } else if (temperature > 24) {
            mWeatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.cloudy));
            base.setBackgroundColor(getResources().getColor(R.color.light_orange));
        } else {
            mWeatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.clouds));
            base.setBackgroundColor(getResources().getColor(R.color.light_blue));
        }
    }

    private void showMessage(String msg) {
        mMessageText.setText(msg);
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.local_service_started);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.mipmap.ic_launcher, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);


        notification.setLatestEventInfo(this, getText(R.string.remote_service_label),
                text, contentIntent);// Send the notification.

        mNM.notify(NOTIFICATION, notification);
    }

    /**
     * Converts a given datagram packet's contents to a String.
     */
    static String stringFromPacket(DatagramPacket packet) {
        return new String(packet.getData(), 0, packet.getLength());
    }


}
