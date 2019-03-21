package com.rdb.download.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.rdb.download.DownloadConfig;
import com.rdb.download.DownloadFileNameBuilder;
import com.rdb.download.DownloadInfo;
import com.rdb.download.DownloadListener;
import com.rdb.download.DownloadManager;
import com.rdb.download.DownloadPartRule;
import com.rdb.download.DownloadStatus;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private View addView;
    private RecyclerView downloadListView;
    private DownloadAdapter downloadAdapter;
    private List<DownloadInfo> downloadInfos;
    private DownloadManager downloadManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DownloadConfig config=new DownloadConfig.Builder()
                .setBufferSize(102400)//片段缓存字节数
                .setMaxTaskCount(3)//最大下载任务数
                .setDownloadPartRule(new DownloadPartRule() {//分段规则
                    @Override
                    public int getPartCounts(int length) {
                        return 1;
                    }
                }).build();
        DownloadManager.init(config);
        downloadManager=DownloadManager.getInstance(this);
        addView = findViewById(R.id.addView);
        addView.setOnClickListener(this);
        downloadListView = findViewById(R.id.downloadListView);
        downloadInfos = DownloadManager.getInstance(this).getDownloadInfos();
        downloadListView.setLayoutManager(new LinearLayoutManager(this));
        downloadAdapter = new DownloadAdapter(this, downloadInfos);
        downloadListView.setAdapter(downloadAdapter);
        checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, 100, "请您授予读取SD卡的权限 否则应用无法运行");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && hasPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            } else {
                Toast.makeText(this, "没有授予SD卡读写权限 应用无法运行", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }


    @Override
    public void onClick(View v) {
        if (v == addView) {
            downloadManager.downloadFile(new File(Environment.getExternalStorageDirectory() + "/Download/"), "https://file.kedududu.com/android/2018/05/18/20180518155926566781123121.apk", DownloadFileNameBuilder.MILLISECOND_WITH_SUFFIX, null);
            downloadInfos.clear();
            downloadInfos.addAll(downloadManager.getDownloadInfos());
            downloadAdapter.notifyDataSetChanged();
        }
    }

    public boolean hasPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean checkPermission(final Activity context, final String permission, final int requestCode, String hint) {
        if (hasPermission(context, permission)) {
            return true;
        } else {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(context, permission)) {
                new AlertDialog.Builder(this).setMessage(hint).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(context, new String[]{permission}, requestCode);
                    }
                }).setCancelable(false).show();
            } else {
                ActivityCompat.requestPermissions(context, new String[]{permission}, requestCode);
            }
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        downloadManager.unregisterDownloadListenerByTag(downloadAdapter);
    }

    private class DownloadHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        private Button pause;
        private Button resume;
        private Button cancel;
        private Button delete1;
        private Button delete2;
        private TextView name;
        private ProgressBar progressBar;
        private DownloadAdapter adapter;
        private DownloadInfo downloadInfo;
        private DownloadListener downloadListenr=new DownloadListener(){
            @Override
            public void onDownloadStart(DownloadInfo downloadInfo) {
                super.onDownloadStart(downloadInfo);
                pause.setVisibility(View.VISIBLE);
                resume.setVisibility(View.GONE);
                cancel.setVisibility(View.VISIBLE);
                delete1.setVisibility(View.GONE);
                delete2.setVisibility(View.GONE);
                pause.setEnabled(true);
            }

            @Override
            public void onDownloadProgress(DownloadInfo downloadInfo) {
                super.onDownloadProgress(downloadInfo);
                progressBar.setMax(downloadInfo.getTotalLength());
                progressBar.setProgress(downloadInfo.getCurLength());
                pause.setVisibility(View.VISIBLE);
                resume.setVisibility(View.GONE);
                cancel.setVisibility(View.VISIBLE);
                delete1.setVisibility(View.GONE);
                delete2.setVisibility(View.GONE);
            }

            @Override
            public void onDownloadPause(DownloadInfo downloadInfo) {
                super.onDownloadPause(downloadInfo);
                pause.setVisibility(View.GONE);
                resume.setVisibility(View.VISIBLE);
                cancel.setVisibility(View.VISIBLE);
                delete1.setVisibility(View.GONE);
                delete2.setVisibility(View.GONE);
                resume.setEnabled(true);
            }

            @Override
            public void onDownloadCancel(DownloadInfo downloadInfo) {
                super.onDownloadCancel(downloadInfo);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onDownloadSuccess(DownloadInfo downloadInfo) {
                super.onDownloadSuccess(downloadInfo);
                pause.setVisibility(View.GONE);
                resume.setVisibility(View.GONE);
                cancel.setVisibility(View.GONE);
                delete1.setVisibility(View.VISIBLE);
                delete2.setVisibility(View.VISIBLE);
            }

            @Override
            public void onDownloadFail(DownloadInfo downloadInfo, Exception e) {
                super.onDownloadFail(downloadInfo, e);
                pause.setVisibility(View.GONE);
                resume.setEnabled(true);
                resume.setVisibility(View.VISIBLE);
                cancel.setVisibility(View.VISIBLE);
                delete1.setVisibility(View.GONE);
                delete2.setVisibility(View.GONE);
            }
        };

        public DownloadHolder(DownloadAdapter adapter,@NonNull View itemView) {
            super(itemView);
            pause =  itemView.findViewById(R.id.pause);
            resume = itemView.findViewById(R.id.resume);
            cancel =itemView.findViewById(R.id.cancel);
            delete1 =itemView.findViewById(R.id.delete1);
            delete2 =  itemView.findViewById(R.id.delete2);
            name = itemView.findViewById(R.id.fileName);
            progressBar =  itemView.findViewById(R.id.progress);
            pause.setOnClickListener(this);
            cancel.setOnClickListener(this);
            resume.setOnClickListener(this);
            delete1.setOnClickListener(this);
            delete2.setOnClickListener(this);
            this.adapter=adapter;
        }

        @Override
        public void onClick(View v) {
            if (v == pause) {
               downloadManager.pauseDownload(downloadInfo);
                pause.setEnabled(false);
            } else if (v == cancel) {
                downloadManager.cancleDownload(downloadInfo);
                downloadInfos.remove(downloadInfo);
                adapter.notifyDataSetChanged();
            } else if (v == resume) {
                downloadManager.resumeDownload(downloadInfo, downloadListenr);
                v.setEnabled(false);
            } else if (v == delete1) {
                downloadManager.deleteDownloadInfo(downloadInfo, false);
                downloadInfos.remove(downloadInfo);
                adapter.notifyDataSetChanged();
            } else if (v == delete2) {
                downloadManager.deleteDownloadInfo(downloadInfo, true);
                downloadInfos.remove(downloadInfo);
                adapter.notifyDataSetChanged();
            }
        }

        public void updateItem(DownloadInfo downloadInfo,int position){
            this.downloadInfo=downloadInfo;
            name.setText(downloadInfo.getFileName());
            if (downloadInfo.getTotalLength() > 0) {
                progressBar.setMax(downloadInfo.getTotalLength());
                progressBar.setProgress(downloadInfo.getCurLength());
            } else {
                progressBar.setMax(100);
                progressBar.setProgress(0);
            }
            downloadManager.unregisterDownloadListener(downloadListenr);
            pause.setEnabled(true);
            resume.setEnabled(true);
            if (downloadInfo.getStatus() == DownloadStatus.WAIT || downloadInfo.getStatus() == DownloadStatus.RUNNING) {
                pause.setVisibility(View.VISIBLE);
                resume.setVisibility(View.GONE);
                cancel.setVisibility(View.VISIBLE);
                delete1.setVisibility(View.GONE);
                delete2.setVisibility(View.GONE);
                downloadManager.registerDownloadListener(downloadInfo, downloadListenr);
            } else if (downloadInfo.getStatus() == DownloadStatus.PAUSE || downloadInfo.getStatus() == DownloadStatus.FAIL) {
                pause.setVisibility(View.GONE);
                resume.setVisibility(View.VISIBLE);
                cancel.setVisibility(View.VISIBLE);
                delete1.setVisibility(View.GONE);
                delete2.setVisibility(View.GONE);
            } else if (downloadInfo.getStatus() == DownloadStatus.SUCCESS) {
                pause.setVisibility(View.GONE);
                resume.setVisibility(View.GONE);
                cancel.setVisibility(View.GONE);
                delete1.setVisibility(View.VISIBLE);
                delete2.setVisibility(View.VISIBLE);
            } else {
                pause.setVisibility(View.GONE);
                resume.setVisibility(View.GONE);
                cancel.setVisibility(View.GONE);
                delete1.setVisibility(View.GONE);
                delete2.setVisibility(View.GONE);
            }
        }
    }
    private class DownloadAdapter extends RecyclerView.Adapter<DownloadHolder> {

        private List<DownloadInfo> list;
        private LayoutInflater inflater;

        public DownloadAdapter(Context context, List<DownloadInfo> list) {
            super();
            inflater=LayoutInflater.from(context);
            this.list=list;
        }

        @NonNull
        @Override
        public DownloadHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View view = inflater.inflate(R.layout.item_download_layout, viewGroup, false);
            return new DownloadHolder(this,view);
        }

        @Override
        public void onBindViewHolder(@NonNull DownloadHolder downloadHolder, int i) {
            DownloadInfo data = list.get(i);
            downloadHolder.updateItem(data, i);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }
}
