package com.example.sensor_android_java;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

enum AccelerationDirection {
    // X軸方向
    X,

    // Y軸方向
    Y,

    // Z軸方向
    Z
}

public class MainActivity extends AppCompatActivity implements SensorEventListener, SurfaceHolder.Callback {

    /// センサー
    private SensorManager manager;
    private Sensor sensor;
    private SurfaceHolder surfaceHolder;

    /// SurfaceView
    private SurfaceView surfaceView;
    private int width;
    private int height;

    /// 半径
    final float radius = 150.f;

    ///係数
    final float coef = 1000.f;

    /// 各座標
    float ballX;
    float ballY;
    float accelerateX;
    float accelerateY;

    /// 加速取得時間
    long accelerateTime;

    /// 画像(Bitmapはswiftで言うUIImage)
    Bitmap ballBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /// 画面を縦固定
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        configureSensor();
        configureSurfaceHolder();
        configureImage();
    }

    /*画面が表示されたら呼ばれる*/
    @Override
    protected void onResume() {
        super.onResume();

    }

    /*画面が閉じれれたら呼ばれる*/
    @Override
    protected void onPause() {
        super.onPause();

    }

    /*センサーの構築*/
    private void configureSensor() {
        /*Androidシステムで用意された様々なサービスを取得するものでその中のSENSOR_SERVICEを使う*/
        manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        /*加速度を表すセンサーを設定*/
        sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    /*SurfaceHolderの構築*/
    private void configureSurfaceHolder() {
        surfaceView = findViewById(R.id.surfaceView2);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        /// surfaceViewを透明にする
        surfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
        surfaceView.setZOrderOnTop(true);

        Paint paint = new Paint();
        paint.setStrokeWidth(20);
        paint.setColor(Color.BLUE);
    }

    private void configureImage() {
        Bitmap ballImage = BitmapFactory.decodeResource(getResources(), R.drawable.ball);
        int dia = (int) radius * 2;
        ballBitmap = Bitmap.createScaledBitmap(ballImage, dia, dia, false);
        ballBitmap = ballImage;
    }

    /*キャンバス*/
    private void drawCanvas() {
        Canvas canvas = surfaceHolder.lockCanvas();

        /// 画面をクリア
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        Paint paint = new Paint();
        canvas.drawBitmap(ballBitmap, ballX - radius, ballY - radius, paint);

        surfaceHolder.unlockCanvasAndPost(canvas);
    }

    // SensorEventListenerメソッド

    /*加速度センサーの値が変更になった際に呼ばれる*/
    @Override
    public void onSensorChanged(SensorEvent event) {
        final int type = event.sensor.getType();
        AccelerationDirection shaft = getAccelerationDirection(event);

        switch (type) {
            /// デバイスの加速度を測定(通常、X、Y、Z軸方向の加速度を提供)
            case Sensor.TYPE_ACCELEROMETER:

                /// 時間を求める
                if (accelerateTime == 0) {
                    accelerateTime = event.timestamp;
                    return;
                }

                float timeTaken = event.timestamp - accelerateTime;
                accelerateTime = event.timestamp;

                /// nsをsに変更
                timeTaken /= 1000000000.f;

                /// 移動距離を求める
                float distanceX = (accelerateX * timeTaken) + (event.values[0] * timeTaken * timeTaken / 2.f);
                float distanceY = (accelerateX * timeTaken) + (event.values[1] * timeTaken * timeTaken / 2.f);

                /// 移動距離からボールの位置を計算
                ballX += distanceX * coef;
                ballY += distanceY * coef;

                /// ボールの移動速度の更新
                distanceX += event.values[0] * timeTaken;
                distanceY += event.values[1] * timeTaken;

                // - Note: 上記の計算に関する物(等加速度運動の公式) https://www.try-it.jp/chapters-8001/sections-8022/lessons-8023/#:~:text=(%E9%80%9F%E5%BA%A6)%EF%BC%9D(%E5%88%9D%E9%80%9F%E5%BA%A6,%E8%A1%A8%E3%81%99%E3%81%93%E3%81%A8%E3%81%8C%E3%81%A7%E3%81%8D%E3%81%BE%E3%81%99%E3%80%82

                boolean isBallXInside = ballX - radius < 0 && distanceX < 0;
                boolean isBallXOutside = ballX + radius > 0 && distanceX > 0;
                boolean isBallYInside = ballY - radius < 0 && distanceY < 0;
                boolean isBallYOutside = ballY + radius > 0 && distanceY > 0;

                /// X内側かどうか
                if (isBallXInside) {
                    distanceX = -distanceX / 1.5f;
                    ballX = radius;
                }

                /// X外側かどうか
                if (isBallXOutside) {
                    distanceX = -distanceX / 1.5f;
                    ballX = width - radius;
                }

                /// Y内側かどうか
                if (isBallYInside) {
                    distanceY = -distanceY / 1.5f;
                    ballY = radius;
                }

                /// Y外側かどうか
                if (isBallYOutside) {
                    distanceY = -distanceY / 1.5f;
                    ballY = height - radius;
                }

                /// キャンバスを再描画
                drawCanvas();

                break;

            /// デバイスの温度を測定(例: 環境温度を提供)
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                break;

            /// 地磁気の強度を測定(デバイスの方位（方向）を決定するのに使用される)
            case Sensor.TYPE_GYROSCOPE:
                break;

            /// 地磁気の強度を測定(デバイスの方位（方向）を決定するのに使用される)
            case Sensor.TYPE_MAGNETIC_FIELD:
                ///
                break;

            /// 周囲の照明レベルを測定(明るさを自動調整するために使用される)
            case Sensor.TYPE_LIGHT:
                break;

            /// 物体の近接を検出(通話中に画面を無効にするために使用される)
            case Sensor.TYPE_PROXIMITY:
                break;

            /// 重力ベクトルの情報を提供(デバイスの傾きや回転を検出)
            case Sensor.TYPE_GRAVITY:
                break;

            /// 直線的な加速度を測定(重力の影響を排除)
            case Sensor.TYPE_LINEAR_ACCELERATION:
                break;

            /// 3D空間内でのデバイスの回転を提供(ジャイロスコープと磁気センサーのデータを組み合わせて計算)
            case Sensor.TYPE_ROTATION_VECTOR:
                break;

            default:
                break;
        }
    }

    /*加速度センサーの制度が変更された時に呼ばれる*/
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /*軸の方向を取得*/
    private AccelerationDirection getAccelerationDirection(SensorEvent event) {
        /// X軸方向の加速度
        float x = event.values[0];

        /// Y軸方向の加速度
        float y = event.values[1];

        /// Z軸方向の加速度
        float z = event.values[2];

        if (Math.abs(x) > Math.abs(y) && Math.abs(x) > Math.abs(z)) {
            /// X軸方向の加速度
            return AccelerationDirection.X;
        } else if (Math.abs(y) > Math.abs(x) && Math.abs(y) > Math.abs(z)) {
            /// Y軸方向の加速度
            return AccelerationDirection.Y;
        } else {
            /// Z軸方向の加速度
            return AccelerationDirection.Z;
        }
    }

    // SurfaceHolderメソッド

    /*画面が作られた時に呼ばれる*/
    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
    }

    /*変更があった時に呼ばれる*/
    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        this.width = width;
        this.height = height;

        /// 初期値
        this.width /= 2;
        this.height /= 2;

        /// 速度関連初期化
        accelerateX = 0;
        accelerateY = 0;
        accelerateTime = 0;
    }

    /*削除される時の呼ばれる*/
    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        manager.unregisterListener(this);
    }
}