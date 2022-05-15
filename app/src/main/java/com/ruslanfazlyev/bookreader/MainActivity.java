package com.ruslanfazlyev.bookreader;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ArrayList<PdfFile> list = new ArrayList<PdfFile>();
    private ListView listView;
    private static final int REQUEST_PERMISSION = 1;


    public class PdfFile {
        private String fileName;
        private String filePath;

        PdfFile(String fileName, String filePath) {
            this.fileName = fileName;
            this.filePath = filePath;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            initViews();
        } else {
            ActivityCompat.requestPermissions(this, new String[] {
                    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, REQUEST_PERMISSION);
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initViews();
                } else {
                    // в разрешении отказано (в первый раз, когда чекбокс "Больше не спрашивать" ещё не показывается)
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        finish();
                    }
                    // в разрешении отказано (выбрано "Больше не спрашивать")
                    else {
                        // показываем диалог, сообщающий о важности разрешения
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setMessage(
                                "Вы отказались предоставлять разрешение на чтение хранилища.\n\nЭто необходимо для работы приложения."
                                        + "\n\n"
                                        + "Нажмите \"Предоставить\", чтобы предоставить приложению разрешения.")
                                // при согласии откроется окно настроек, в котором пользователю нужно будет вручную предоставить разрешения
                                .setPositiveButton("Предоставить", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        finish();
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.fromParts("package", getPackageName(), null));
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                    }
                                })
                                // закрываем приложение
                                .setNegativeButton("Отказаться", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        finish();
                                    }
                                });
                        builder.setCancelable(false);
                        builder.create().show();
                    }
                }
                break;
            }
        }
    }

    private BaseAdapter adapter = new BaseAdapter() {
        @Override public int getCount() {
            return list.size();
        }

        @Override public PdfFile getItem(int i) {
            return (PdfFile) list.get(i);
        }

        @Override public long getItemId(int i) {
            return i;
        }

        @Override public View getView(int i, View view, ViewGroup viewGroup) {
            View v = view;
            if (v == null) {
                v = getLayoutInflater().inflate(R.layout.list_item, viewGroup, false);
            }

            PdfFile pdfFile = getItem(i);
            TextView name = v.findViewById(R.id.txtFileName);
            name.setText(pdfFile.getFileName());
            return v;
        }
    };

    private void initViews() {
        // получаем путь до внешнего хранилища
        String path = Environment.getExternalStorageDirectory().getAbsolutePath();

        initList(path);
        // устанавливаем адаптер в ListView
        listView.setAdapter(adapter);
        // когда пользователь выбирает PDF-файл из списка, открываем активность для просмотра
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(MainActivity.this, PdfActivity.class);
                intent.putExtra("keyName", list.get(i).getFileName());
                intent.putExtra("fileName", list.get(i).getFilePath());
                startActivity(intent);
            }
        });
    }

    private void initList(String path) {
        try {

            File file = new File(path);
            if (file.exists()){
                File[] fileList = file.listFiles();
                String fileName;
                for (File f : fileList) {
                    if (f.isDirectory()) {
                        initList(f.getAbsolutePath());
                    } else {
                        fileName = f.getName();
                        if (fileName.endsWith(".pdf")) {
                            list.add(new PdfFile(fileName, f.getAbsolutePath()));
                        }
                    }
                }
            }else{
                Toast.makeText(this, "dead", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
        listView = findViewById(R.id.listView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission();
            initViews();
        } else {
            initViews();
        }
    }
}