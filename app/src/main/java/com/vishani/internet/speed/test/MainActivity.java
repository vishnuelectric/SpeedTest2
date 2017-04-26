package com.vishani.internet.speed.test;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.anastr.speedviewlib.PointerSpeedometer;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.NativeExpressAdView;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static final String COUNT= "count";
    private static final int DOWNLOAD_PROGRESS = 1 ;
    private static final int UPLOAD_PROGRESS =2 ;
    private static final int SPEED_TEST_ERROR =  3;
    private static final int DOWNLOAD_FINISHED =  4;
    private static final int UPLOAD_FINISHED =  5;
    private static final int TEST_INTERRUPT =  6;
    private WebView mWebView;
    private ProgressDialog mProgressDialog;
    private SharedPreferences mPreference;
     TextView mDownloadTxv,mUploadTextView;
    PointerSpeedometer pointerSpeedometer;
    Button mTestAgain;
    private HandlerThread mHandlerThread;
    private Handler mUIHandler,mWorkerHandler;
    SpeedTestSocket speedTestSocket;
    private  boolean mDownloadStarted,mDownloadFinished,mUploadStarted,mUploadFinished;
    private Runnable mWorkerSpeedRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mDownloadFinished) {
                speedTestSocket.startDownload("speedtestggn1.airtel.in", "/speedtest/random2000x2000.jpg");
            } else {
                speedTestSocket.startUpload("speedtestggn1.airtel.in", "/speedtest/upload.php", 1000000);
            }

        }
    };
    private NativeExpressAdView nativeExpressAdView;
    private RatingBar mRatingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
       mDownloadTxv= (TextView)findViewById(R.id.download_speed_txv);
        mUploadTextView= (TextView)findViewById(R.id.upload_speed_txv);
       mTestAgain = (Button)findViewById(R.id.test_again_btn) ;
        mPreference = PreferenceManager.getDefaultSharedPreferences(this);
        nativeExpressAdView = (NativeExpressAdView)findViewById(R.id.maiAd);
        nativeExpressAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
            }

            @Override
            public void onAdFailedToLoad(int i) {
                super.onAdFailedToLoad(i);
            }
        });
        nativeExpressAdView.loadAd(new AdRequest.Builder().addTestDevice("F07C4CCDC55C7CF6AE8E029E03ED7C75").build());
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, "I checked my internet's true speed, Now you too can, Download this app for free https://play.google.com/store/apps/details?id=com.vishani.internet.speed.test");
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, "Choose one"));
            }
        });
        mHandlerThread = new HandlerThread("MyHandlerThread");
        mHandlerThread.start();

        mUIHandler= new Handler(Looper.getMainLooper())
        {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what)
                {
                    case DOWNLOAD_PROGRESS:
                        mDownloadTxv.setText("Download: Testing...");
                        mTestAgain.setVisibility(View.GONE);
                        pointerSpeedometer.speedTo(Float.valueOf((String) msg.obj),100);
                        break;
                    case UPLOAD_PROGRESS:
                        mUploadTextView.setText("Upload: Testing...");
                        mTestAgain.setVisibility(View.GONE);
                        pointerSpeedometer.speedTo(Float.valueOf((String) msg.obj),100);

                        break;
                    case SPEED_TEST_ERROR:
                        Toast.makeText(MainActivity.this,"SPeed test error,Try again",Toast.LENGTH_LONG).show();
                        mTestAgain.setVisibility(View.VISIBLE);

                        break;

                    case DOWNLOAD_FINISHED:
                        mDownloadTxv.setText("Download: "+msg.obj+"Mbps");
                        mTestAgain.setVisibility(View.VISIBLE);
                        pointerSpeedometer.speedTo(Float.valueOf((String) msg.obj),100);

                        break;

                    case UPLOAD_FINISHED:
                        mUploadTextView.setText("Upload: "+msg.obj+"Mbps");
                        mTestAgain.setVisibility(View.VISIBLE);
                        pointerSpeedometer.speedTo(0f,200);
                        Toast.makeText(MainActivity.this,"Speed Test Finished",Toast.LENGTH_LONG).show();



                        break;
                    case TEST_INTERRUPT:
                        Toast.makeText(MainActivity.this,"SPeed test interrupted",Toast.LENGTH_LONG).show();

                        break;

                }

            }
        };

        mRatingBar = (RatingBar) findViewById(R.id.rating_bar);
        if(mPreference.getInt("launch_count",0) > 16)
        {
            mRatingBar.setVisibility(View.GONE);
        }
        mRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float v, boolean b) {
                if(ratingBar.getRating()>= 4 )
                {
                    Toast.makeText(ratingBar.getContext(),"If you like this app please rate us 5 star",Toast.LENGTH_LONG).show();
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.vishani.internet.speed.test")));
                    mPreference.edit().putInt("launch_count",16).apply();

                }else
                {

                    mPreference.edit().putInt("launch_count",16).apply();

                }
            }
        });
         pointerSpeedometer= (PointerSpeedometer) findViewById(R.id.pointerSpeedometer);
        pointerSpeedometer.setUnit("Mbps");
        pointerSpeedometer.setWithTremble(false);

         speedTestSocket = new SpeedTestSocket();
        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onDownloadFinished(SpeedTestReport report) {
                Message message = Message.obtain(mUIHandler,DOWNLOAD_FINISHED,String.valueOf(Math.round(report.getTransferRateBit().floatValue()/1000000 *100f)/100f));
                mUIHandler.sendMessage(message);
                mDownloadFinished =true;
                mWorkerHandler.post(mWorkerSpeedRunnable);

            }

            @Override
            public void onDownloadProgress(float percent, SpeedTestReport report) {
                Message message = Message.obtain(mUIHandler,DOWNLOAD_PROGRESS,String.valueOf(Math.round(report.getTransferRateBit().floatValue()/1000000 *100f)/100f));
                mDownloadFinished =false;
                mDownloadStarted=true;
                mUIHandler.sendMessage(message);
            }

            @Override
            public void onDownloadError(SpeedTestError speedTestError, String errorMessage) {
                System.out.println(errorMessage);
                mDownloadFinished= true;
                Message message = Message.obtain(mUIHandler,SPEED_TEST_ERROR,errorMessage);
                mUIHandler.sendMessage(message);

            }

            @Override
            public void onUploadFinished(SpeedTestReport report) {
                System.out.println("upload"+ report);
                mDownloadFinished =false;
                mUploadFinished =true;
                Message message = Message.obtain(mUIHandler,UPLOAD_FINISHED,String.valueOf(Math.round(report.getTransferRateBit().floatValue()/1000000 *100f)/100f));
           mUIHandler.sendMessage(message);
            }

            @Override
            public void onUploadError(SpeedTestError speedTestError, String errorMessage) {
                mUploadFinished =true;
                Message message = Message.obtain(mUIHandler,SPEED_TEST_ERROR,errorMessage);
                mUIHandler.sendMessage(message);


            }

            @Override
            public void onUploadProgress(float percent, SpeedTestReport report) {
                mUploadStarted= true;
                mDownloadStarted= false;
                Message message = Message.obtain(mUIHandler,UPLOAD_PROGRESS,String.valueOf(Math.round(report.getTransferRateBit().floatValue()/1000000 *100f)/100f));
                  mUIHandler.sendMessage(message);
            }

            @Override
            public void onInterruption() {
                Message message = Message.obtain(mUIHandler,TEST_INTERRUPT);
                mUIHandler.sendMessage(message);


            }
        });
        if(mHandlerThread.isAlive() && mHandlerThread.getLooper() != null) {
            mWorkerHandler = new Handler(mHandlerThread.getLooper());

        }

        mWorkerHandler.post(mWorkerSpeedRunnable);
mTestAgain.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        if(mHandlerThread.isAlive() && mHandlerThread.getLooper() != null) {
            mWorkerHandler = new Handler(mHandlerThread.getLooper());
            mWorkerHandler.post(mWorkerSpeedRunnable);
        }
        if(mPreference.getInt("launch_count",0) > 2 && mPreference.getInt("launch_count",0) <15)
        {
            showRatingDialog();
        }
        int count = mPreference.getInt("launch_count",0);
        count = count+ 1;
        mPreference.edit().putInt("launch_count",count).apply();
        mTestAgain.setVisibility(View.GONE);
        mDownloadFinished = false;
        mDownloadStarted=false;
        mUploadStarted=false;
        mUploadFinished=false;
    }
});
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);



    }

    @Override
    protected void onStart() {
        super.onStart();




    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mHandlerThread.quitSafely();
        }
        else {
            mHandlerThread.quit();
        }
        if(speedTestSocket != null) {
            speedTestSocket.forceStopTask();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if(id == R.id.battery_saver)
        {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.vishani.batterywidget")));

        }
        if(id == R.id.nav_share)
        {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, "I checked my internet's true speed, Now you too can, Download this app for free https://play.google.com/store/apps/details?id=com.vishani.internet.speed.test");
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, "Choose one"));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    private void showRatingDialog()
    {
        new AlertDialog.Builder(this).setTitle("Like this app").setMessage("If you like this app please rate us 5 star")
                .setPositiveButton("Alright", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.vishani.internet.speed.test")));
                        mPreference.edit().putInt("launch_count",16).apply();

                    }
                })
                .setNegativeButton("Not now", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setNeutralButton("Already Rated", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        mPreference.edit().putInt("launch_count",16).apply();

                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {

                    }
                })
                .setCancelable(false)
                .show();
    }





}
