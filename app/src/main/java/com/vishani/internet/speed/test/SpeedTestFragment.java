package com.vishani.internet.speed.test;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import com.github.anastr.speedviewlib.PointerSpeedometer;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.NativeExpressAdView;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

/**
 * Created by VISHNUPRASAD on 02/02/17.
 */

public class SpeedTestFragment extends Fragment {
    private static final String COUNT= "count";
    private static final int DOWNLOAD_PROGRESS = 1 ;
    private static final int UPLOAD_PROGRESS =2 ;
    private static final int SPEED_TEST_ERROR =  3;
    private static final int DOWNLOAD_FINISHED =  4;
    private static final int UPLOAD_FINISHED =  5;
    private static final int TEST_INTERRUPT =  6;
    private WebView mWebView;
    private NativeExpressAdView nativeExpressAdView;
    private ProgressDialog mProgressDialog;
    private SharedPreferences mPreference;
    TextView mDownloadTxv,mUploadTextView;
    PointerSpeedometer pointerSpeedometer;
    Button mTestAgain;
    Runnable mWorkerSpeedRunnable = null;
    private HandlerThread mHandlerThread;
    private Handler mUIHandler,mWorkerHandler;
    SpeedTestSocket speedTestSocket;
    private  boolean mDownloadStarted,mDownloadFinished,mUploadStarted,mUploadFinished;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.content_main,container,false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mHandlerThread = new HandlerThread("MyHandlerThread");
        mHandlerThread.start();


        mUIHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case DOWNLOAD_PROGRESS:
                        mDownloadTxv.setText("Download: Testing...");
                        mTestAgain.setVisibility(View.GONE);
                        pointerSpeedometer.speedTo(Float.valueOf((String) msg.obj), 100);
                        break;
                    case UPLOAD_PROGRESS:
                        mUploadTextView.setText("Upload: Testing...");
                        mTestAgain.setVisibility(View.GONE);
                        pointerSpeedometer.speedTo(Float.valueOf((String) msg.obj), 100);

                        break;
                    case SPEED_TEST_ERROR:
                        mTestAgain.setVisibility(View.VISIBLE);

                        break;

                    case DOWNLOAD_FINISHED:
                        mDownloadTxv.setText("Download: " + msg.obj + "Mbps");
                        mTestAgain.setVisibility(View.VISIBLE);
                        pointerSpeedometer.speedTo(Float.valueOf((String) msg.obj), 100);

                        break;

                    case UPLOAD_FINISHED:
                        mUploadTextView.setText("Upload: " + msg.obj + "Mbps");
                        mTestAgain.setVisibility(View.VISIBLE);
                        pointerSpeedometer.speedTo(0f, 200);


                        break;
                    case TEST_INTERRUPT:

                        break;

                }

            }
        };


        pointerSpeedometer = (PointerSpeedometer) view.findViewById(R.id.pointerSpeedometer);
        pointerSpeedometer.setUnit("Mbps");
        pointerSpeedometer.setWithTremble(false);

        speedTestSocket = new SpeedTestSocket();
        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onDownloadFinished(SpeedTestReport report) {
                Message message = Message.obtain(mUIHandler, DOWNLOAD_FINISHED, String.valueOf(Math.round(report.getTransferRateBit().floatValue() / 1000000 * 100f) / 100f));
                mUIHandler.sendMessage(message);
                mDownloadFinished = true;
                mWorkerHandler.post(mWorkerSpeedRunnable);

            }

            @Override
            public void onDownloadProgress(float percent, SpeedTestReport report) {
                Message message = Message.obtain(mUIHandler, DOWNLOAD_PROGRESS, String.valueOf(Math.round(report.getTransferRateBit().floatValue() / 1000000 * 100f) / 100f));
                mDownloadFinished = false;
                mDownloadStarted = true;
                mUIHandler.sendMessage(message);
            }

            @Override
            public void onDownloadError(SpeedTestError speedTestError, String errorMessage) {
                System.out.println(errorMessage);
                mDownloadFinished = true;
                Message message = Message.obtain(mUIHandler, SPEED_TEST_ERROR, errorMessage);
                mUIHandler.sendMessage(message);

            }

            @Override
            public void onUploadFinished(SpeedTestReport report) {
                System.out.println("upload" + report);
                mDownloadFinished = false;
                mUploadFinished = true;
                Message message = Message.obtain(mUIHandler, UPLOAD_FINISHED, String.valueOf(Math.round(report.getTransferRateBit().floatValue() / 1000000 * 100f) / 100f));
                mUIHandler.sendMessage(message);
            }

            @Override
            public void onUploadError(SpeedTestError speedTestError, String errorMessage) {
                mUploadFinished = true;
                Message message = Message.obtain(mUIHandler, SPEED_TEST_ERROR, errorMessage);
                mUIHandler.sendMessage(message);


            }

            @Override
            public void onUploadProgress(float percent, SpeedTestReport report) {
                mUploadStarted = true;
                mDownloadStarted = false;
                Message message = Message.obtain(mUIHandler, UPLOAD_PROGRESS, String.valueOf(Math.round(report.getTransferRateBit().floatValue() / 1000000 * 100f) / 100f));
                mUIHandler.sendMessage(message);
            }

            @Override
            public void onInterruption() {
                Message message = Message.obtain(mUIHandler, TEST_INTERRUPT);
                mUIHandler.sendMessage(message);


            }
        });
        if (mHandlerThread.isAlive() && mHandlerThread.getLooper() != null) {
            mWorkerHandler = new Handler(mHandlerThread.getLooper());

        }


        mWorkerHandler.post(mWorkerSpeedRunnable);
        mTestAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mHandlerThread.isAlive() && mHandlerThread.getLooper() != null) {
                    mWorkerHandler = new Handler(mHandlerThread.getLooper());

                }
                if (mPreference.getInt("launch_count", 0) > 2 && mPreference.getInt("launch_count", 0) < 15) {
                    //showRatingDialog();
                }
                int count = mPreference.getInt("launch_count", 0);
                count = count + 1;
                mPreference.edit().putInt("launch_count", count).apply();
                mTestAgain.setVisibility(View.GONE);
                mDownloadFinished = false;
                mDownloadStarted = false;
                mUploadStarted = false;
                mUploadFinished = false;
            }
        });

        mWorkerSpeedRunnable = new Runnable() {
            @Override
            public void run() {
                if (!mDownloadFinished) {
                    speedTestSocket.startDownload("speedtestggn1.airtel.in", "/speedtest/random2000x2000.jpg");
                } else {
                    speedTestSocket.startUpload("speedtestggn1.airtel.in", "/speedtest/upload.php", 1000000);
                }

            }
        };
    }
}
