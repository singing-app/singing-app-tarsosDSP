package com.example.prj_singer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.UniversalAudioInputStream;
import be.tarsos.dsp.io.android.AndroidAudioPlayer;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.writer.WriterProcessor;

public class MainActivity extends AppCompatActivity {
    AudioDispatcher dispatcher;
    TarsosDSPAudioFormat tarsosDSPAudioFormat;
    File file;

    TextView pitchTextView;
    Button recordButton;
    // Button playButton;
    CheckBox highPitchCheckbox;

    boolean isRecording = false;
    String filename = "recorded_sound.wav";

    private LineChart chart;

    static int time = 0;
    public void setTime(int v){
        time = v;
    }
    public int getTime(){
        return time;
    }
    final int timeLimit = 3;

    Timer timer = new Timer();
    static TimerTask task;
    int remainingTime = 0;
    float userHighPitchAvg = 0;

    private TimerTask mkTimerTask() {
        setTime(0);
        TimerTask tempTesk = new TimerTask() {
            @Override
            public void run() {
                remainingTime = timeLimit - getTime();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pitchTextView.setText(" 낼 수 있는 최고음을 3초간 유지하세요: " + remainingTime);
                    }
                });
                time++;
                if(time > timeLimit){
                    isRecording = false;
                    setTime(0);
                    userHighPitchAvg = calcHighPitchAvg(recordPitchList);
                    recordButton.setText("측정 시작");
                    stopRecording();
                }
            }
        };
        return tempTesk;
    }

    public float highPitchAvg = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // timer.purge();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chart = (LineChart) findViewById(R.id.LineChart);

        chart.setDrawGridBackground(true);
        chart.setBackgroundColor(Color.BLACK);
        chart.setGridBackgroundColor(Color.BLACK);

// description text
        chart.getDescription().setEnabled(true);
        Description des = chart.getDescription();
        des.setEnabled(true);
        des.setText("Real-Time DATA");
        des.setTextSize(15f);
        des.setTextColor(Color.WHITE);

// touch gestures (false-비활성화)
        chart.setTouchEnabled(false);

// scaling and dragging (false-비활성화)
        chart.setDragEnabled(false);
        chart.setScaleEnabled(false);

//auto scale
        chart.setAutoScaleMinMaxEnabled(true);

// if disabled, scaling can be done on x- and y-axis separately
        chart.setPinchZoom(false);

//X축
        chart.getXAxis().setDrawGridLines(true);
        chart.getXAxis().setDrawAxisLine(false);

        chart.getXAxis().setEnabled(true);
        chart.getXAxis().setDrawGridLines(false);

        //Legend
        Legend l = chart.getLegend();
        l.setEnabled(true);
        l.setFormSize(10f); // set the size of the legend forms/shapes
        l.setTextSize(12f);
        l.setTextColor(Color.WHITE);

        //Y축
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setEnabled(true);
        leftAxis.setTextColor(getResources().getColor(R.color.GREEN));
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(getResources().getColor(R.color.GREEN));

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

// don't forget to refresh the drawing
        chart.invalidate();

        File sdCard = Environment.getExternalStorageDirectory();
        file = new File(sdCard, filename);

    /*
    filePath = file.getAbsolutePath();
    Log.e("MainActivity", "저장 파일 경로 :" + filePath); // 저장 파일 경로 : /storage/emulated/0/recorded.mp4
    */

        tarsosDSPAudioFormat = new TarsosDSPAudioFormat(TarsosDSPAudioFormat.Encoding.PCM_SIGNED,
                22050,
                2 * 8,
                1,
                2 * 1,
                22050,
                ByteOrder.BIG_ENDIAN.equals(ByteOrder.nativeOrder()));

        pitchTextView = findViewById(R.id.pitchTextView);
        recordButton = findViewById(R.id.recordButton);
        // playButton = findViewById(R.id.playButton);
        highPitchCheckbox = findViewById(R.id.highPitchCheckbox);

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    recordAudio();
                    isRecording = true;
                    recordButton.setText("측정 중단");
                } else {
                    stopRecording();
                    isRecording = false;
                    recordButton.setText("측정 시작");
                }
            }
        });

        /* playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playAudio();
            }
        }); */
    }

    public void addEntry(float num) {

        LineData data = chart.getData();

        if (data == null) {
            data = new LineData();
            chart.setData(data);
        }

        ILineDataSet set = data.getDataSetByIndex(0);
        // set.addEntry(...); // can be called as well

        if (set == null) {
            set = createSet();
            data.addDataSet(set);
        }

        data.addEntry(new Entry((float)set.getEntryCount(), num), 0);
        data.notifyDataChanged();

        // let the chart know it's data has changed
        chart.notifyDataSetChanged();

        chart.setVisibleXRangeMaximum(150);
        // this automatically refreshes the chart (calls invalidate())
        chart.moveViewTo(data.getEntryCount(), 50f, YAxis.AxisDependency.LEFT);
    }

    public LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, "Real-time Line Data");
        set.setLineWidth(1f);
        set.setDrawValues(false);
        set.setValueTextColor(getResources().getColor(R.color.white));
        set.setColor(getResources().getColor(R.color.white));
        set.setMode(LineDataSet.Mode.LINEAR);
        set.setDrawCircles(false);
        set.setHighLightColor(Color.rgb(190, 190, 190));

        return set;
    }

    ArrayList<Float> recordPitchList = new ArrayList<Float>();
    public float calcHighPitchAvg(ArrayList<Float> arr){
        if (arr.isEmpty()) { return -1; }

        final int size = arr.size();
        float result = 0;

        for(int i = 0; i < size; i++){
            result += arr.get(i);
        }
        result /= size;
        return result;
    }

    public void recordAudio() {
        try {
            releaseDispatcher();

            FileInputStream fileInputStream = new FileInputStream(file);
            dispatcher = new AudioDispatcher(new UniversalAudioInputStream(fileInputStream, tarsosDSPAudioFormat), 1024, 0);

            AudioProcessor playerProcessor = new AndroidAudioPlayer(tarsosDSPAudioFormat, 2048, 0);
            dispatcher.addAudioProcessor(playerProcessor);


            if (highPitchCheckbox.isChecked()){
                recordPitchList.clear();
                task = mkTimerTask();
                timer.scheduleAtFixedRate(task, 0, 1000);
            }
            PitchDetectionHandler pitchDetectionHandler = new PitchDetectionHandler() {
                @Override
                public void handlePitch(PitchDetectionResult res, AudioEvent e) {
                    final float pitchInHz = res.getPitch();
                    final float datanum = pitchInHz;

                    if (highPitchCheckbox.isChecked()){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                addEntry(datanum);
                                recordPitchList.add(datanum);
                            }
                        });
                    }

                    else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pitchTextView.setText(pitchInHz + "");
                                addEntry(datanum);
                            }
                        });
                    }
                }
            };

            Log.d("myTag", "This is my message");


            AudioProcessor pitchProcessor = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050, 1024, pitchDetectionHandler);
            dispatcher.addAudioProcessor(pitchProcessor);

            Thread audioThread = new Thread(dispatcher, "Audio Thread");
            audioThread.start();

            /* if(highPitchCheckbox.isChecked() && (getTime() >= timeLimit)){
                highPitchAvg = calcHighPitchAvg(datanumList);
                pitchTextView.setText("최고음: " + highPitchAvg);
                stopRecording();
                // timer.interrupt();


            } */

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* public void playAudio() {
        releaseDispatcher();
        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0);

        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            AudioProcessor recordProcessor = new WriterProcessor(tarsosDSPAudioFormat, randomAccessFile);
            dispatcher.addAudioProcessor(recordProcessor);

            PitchDetectionHandler pitchDetectionHandler = new PitchDetectionHandler() {
                @Override
                public void handlePitch(PitchDetectionResult res, AudioEvent e) {
                    final float pitchInHz = res.getPitch();
                    datanum = pitchInHz;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pitchTextView.setText(pitchInHz + "");
                            addEntry(datanum);
                        }
                    });
                }
            };

            AudioProcessor pitchProcessor = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050, 1024, pitchDetectionHandler);
            dispatcher.addAudioProcessor(pitchProcessor);

            Thread audioThread = new Thread(dispatcher, "Audio Thread");
            audioThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    } */

    public void stopRecording() {
        if(task != null) {
            task.cancel();
        }
        releaseDispatcher();
    }

    public void releaseDispatcher() {
        if (dispatcher != null) {
            if (!dispatcher.isStopped())
                dispatcher.stop();
            dispatcher = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseDispatcher();
    }
}