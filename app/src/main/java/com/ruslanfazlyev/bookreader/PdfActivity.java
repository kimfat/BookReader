package com.ruslanfazlyev.bookreader;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;

// класс отвечает за отображеня самой книги и ее манипуляции

public class PdfActivity extends AppCompatActivity {


    // переменные для хранения пути, номера и содержания страницы, масштаба и кнопок
    private String path;
    private ImageView imgView;
    private Button btnPrevious, btnNext;
    private int currentPage = 0;
    private ImageButton btn_zoomin, btn_zoomout;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // находим название и путь необходимой книги
        path = getIntent().getStringExtra("fileName");
        setTitle(getIntent().getStringExtra("keyName"));

        // если в банлде есть номер страницы - забираем его
        if (savedInstanceState != null) {
            currentPage = savedInstanceState.getInt("CURRENT_PAGE", 0);
        }

        // устанавливаем кнопки интерфейса
        imgView = findViewById(R.id.imgView);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        btn_zoomin = findViewById(R.id.zoomin);
        btn_zoomout = findViewById(R.id.zoomout);
        // устанавливаем слушатели на кнопки
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.btnPrevious: {
                        // получаем индекс предыдущей страницы
                        int index = curPage.getIndex() - 1;
                        displayPage(index);
                        break;
                    }
                    case R.id.btnNext: {
                        // получаем индекс следующей страницы
                        int index = curPage.getIndex() + 1;
                        displayPage(index);
                        break;
                    }
                    case R.id.zoomout: {
                        // уменьшаем зум
                        --currentZoomLevel;
                        displayPage(curPage.getIndex());
                        break;
                    }
                    case R.id.zoomin: {
                        // увеличиваем зум
                        ++currentZoomLevel;
                        displayPage(curPage.getIndex());
                        break;
                    }
                }
            }
        };
        btnPrevious.setOnClickListener(clickListener);
        btnNext.setOnClickListener(clickListener);
        btn_zoomin.setOnClickListener(clickListener);
        btn_zoomout.setOnClickListener(clickListener);


    }

    // переменные отображения и хранения рендера книги
    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page curPage;
    private ParcelFileDescriptor descriptor;
    private float currentZoomLevel = 5;

    // в начале пытаемся отркыть книгу
    @Override public void onStart() {
        super.onStart();
        try {
            openPdfRenderer();
            displayPage(currentPage);
        } catch (Exception e) {
            // при возникшей ошибке уведомляем о ней
            Toast.makeText(this, "PDF-файл защищен паролем или другая ошибка.", Toast.LENGTH_SHORT).show();
        }
    }

    //
    private void openPdfRenderer() {
        File file = new File(path);
        descriptor = null;
        pdfRenderer = null;
        try {
            descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(descriptor);
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка", Toast.LENGTH_LONG).show();
        }
    }

    // Функция отображения страницы книги
    private void displayPage(int index) {
        if (pdfRenderer.getPageCount() <= index) return;
        // закрываем текущую страницу
        if (curPage != null) curPage.close();
        // открываем нужную страницу
        curPage = pdfRenderer.openPage(index);

        // выбираем масштаб текущей страницы
        int newWidth = (int) (getResources().getDisplayMetrics().widthPixels * curPage.getWidth() / 72
                * currentZoomLevel / 40);//45
        int newHeight = (int) (getResources().getDisplayMetrics().heightPixels * curPage.getHeight() / 72
                * currentZoomLevel / 65);//90

        // создаем рендер страницы и сохраняем ее
        Bitmap bitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
        Matrix matrix = new Matrix();
        float dpiAdjustedZoomLevel = currentZoomLevel * DisplayMetrics.DENSITY_MEDIUM
                / getResources().getDisplayMetrics().densityDpi;
        matrix.setScale(dpiAdjustedZoomLevel, dpiAdjustedZoomLevel);
        curPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        // отображаем результат рендера
        imgView.setImageBitmap(bitmap);
        // проверяем, нужно ли делать кнопки недоступными при первой и последней страницы
        int pageCount = pdfRenderer.getPageCount();
        btnPrevious.setEnabled(0 != index);
        btnNext.setEnabled(index + 1 < pageCount);
        btn_zoomout.setEnabled(currentZoomLevel != 2);
        btn_zoomin.setEnabled(currentZoomLevel != 12);
    }


    // перезаписываем функции
    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (curPage != null) {
            outState.putInt("CURRENT_PAGE", curPage.getIndex());
        }
    }

    @Override public void onStop() {
        try {
            closePdfRenderer();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onStop();
    }

    // закрыаем необходимые объекты при закрытии книги
    private void closePdfRenderer() throws IOException {
        if (curPage != null) curPage.close();
        if (pdfRenderer != null) pdfRenderer.close();
        if (descriptor != null) descriptor.close();
    }
}
