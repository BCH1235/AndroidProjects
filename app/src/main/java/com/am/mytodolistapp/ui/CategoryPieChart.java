package com.am.mytodolistapp.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

public class CategoryPieChart extends View {

    private Paint piePaint;
    private Paint textPaint;
    private List<StatisticsViewModel.CategoryStatData> data;
    private RectF pieRect;

    public CategoryPieChart(Context context) {
        super(context);
        init();
    }

    public CategoryPieChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CategoryPieChart(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        piePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(36f);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextAlign(Paint.Align.CENTER);

        pieRect = new RectF();
    }

    public void updateData(List<StatisticsViewModel.CategoryStatData> newData) {
        this.data = newData;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (data == null || data.isEmpty()) {
            // 데이터가 없을 때 표시할 메시지
            canvas.drawText(
                    "완료되지 않은 작업이 없습니다",
                    getWidth() / 2f,
                    getHeight() / 2f,
                    textPaint
            );
            return;
        }

        int width = getWidth();
        int height = getHeight();
        int size = Math.min(width, height);
        int radius = size / 3;

        // 파이 차트 중심점
        float centerX = width / 2f;
        float centerY = height / 2f;

        // 파이 차트 영역 설정
        pieRect.set(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius
        );

        // 총 개수 계산
        int totalCount = 0;
        for (StatisticsViewModel.CategoryStatData item : data) {
            totalCount += item.getCount();
        }

        if (totalCount == 0) return;

        // 파이 차트 그리기
        float startAngle = 0;
        for (StatisticsViewModel.CategoryStatData item : data) {
            float sweepAngle = (float) item.getCount() / totalCount * 360;

            try {
                piePaint.setColor(Color.parseColor(item.getColor()));
            } catch (Exception e) {
                piePaint.setColor(Color.GRAY);
            }

            canvas.drawArc(pieRect, startAngle, sweepAngle, true, piePaint);
            startAngle += sweepAngle;
        }
    }
}