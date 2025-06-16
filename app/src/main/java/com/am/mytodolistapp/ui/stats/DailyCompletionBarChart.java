package com.am.mytodolistapp.ui.stats;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

public class DailyCompletionBarChart extends View {

    private Paint barPaint;
    private Paint textPaint;
    private Paint gridPaint;
    private Paint valuePaint;
    private List<StatisticsViewModel.DailyCompletionData> data;

    // 고정 최대값 10으로 설정
    private static final int MAX_VALUE = 10;
    private static final int[] Y_GRID_VALUES = {2, 4, 6, 8, 10};

    public DailyCompletionBarChart(Context context) {
        super(context);
        init();
    }

    public DailyCompletionBarChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DailyCompletionBarChart(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(Color.parseColor("#6366F1")); // 보라색

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(32f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.parseColor("#666666"));

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#E5E5E5"));
        gridPaint.setStrokeWidth(2f);

        valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setTextSize(24f);
        valuePaint.setTextAlign(Paint.Align.RIGHT);
        valuePaint.setColor(Color.parseColor("#999999"));
    }

    public void updateData(List<StatisticsViewModel.DailyCompletionData> newData) {
        this.data = newData;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (data == null || data.isEmpty()) {
            return;
        }

        int width = getWidth();
        int height = getHeight();
        int paddingLeft = 80;  // Y축 숫자 공간
        int paddingRight = 40;
        int paddingTop = 40;
        int paddingBottom = 80; // 요일 텍스트 공간

        int chartHeight = height - paddingTop - paddingBottom;
        int chartWidth = width - paddingLeft - paddingRight;

        // 배경을 흰색으로
        canvas.drawColor(Color.WHITE);

        // Y축 그리드 라인과 값 표시
        for (int value : Y_GRID_VALUES) {
            float y = paddingTop + chartHeight - (float) value / MAX_VALUE * chartHeight;

            // 그리드 라인
            canvas.drawLine(paddingLeft, y, paddingLeft + chartWidth, y, gridPaint);

            // Y축 값 표시
            canvas.drawText(String.valueOf(value), paddingLeft - 20, y + 8, valuePaint);
        }

        // 막대 그래프 그리기
        int barWidth = chartWidth / data.size();
        int barSpacing = barWidth / 4;
        int actualBarWidth = barWidth - barSpacing;

        for (int i = 0; i < data.size(); i++) {
            StatisticsViewModel.DailyCompletionData item = data.get(i);

            float x = paddingLeft + i * barWidth + barSpacing / 2f;
            float barBottom = paddingTop + chartHeight;

            // 실제 데이터 막대
            if (item.getCompletedCount() > 0) {
                float barHeightRatio = Math.min((float) item.getCompletedCount() / MAX_VALUE, 1.0f);
                float barHeight = chartHeight * barHeightRatio;

                RectF dataBar = new RectF(x, barBottom - barHeight, x + actualBarWidth, barBottom);
                canvas.drawRoundRect(dataBar, 8, 8, barPaint);

                // 막대 위에 값 표시
                Paint valueOnBarPaint = new Paint(textPaint);
                valueOnBarPaint.setColor(Color.parseColor("#6366F1"));
                valueOnBarPaint.setTextSize(28f);
                valueOnBarPaint.setFakeBoldText(true);

                canvas.drawText(
                        String.valueOf(item.getCompletedCount()),
                        x + actualBarWidth / 2f,
                        barBottom - barHeight - 15,
                        valueOnBarPaint
                );
            }

            // 요일 텍스트
            canvas.drawText(
                    item.getDayOfWeek(),
                    x + actualBarWidth / 2f,
                    height - paddingBottom / 2f,
                    textPaint
            );
        }

        // 0 라인
        Paint axisPaint = new Paint(gridPaint);
        axisPaint.setColor(Color.parseColor("#CCCCCC"));
        axisPaint.setStrokeWidth(3f);
        canvas.drawLine(paddingLeft, paddingTop + chartHeight, paddingLeft + chartWidth, paddingTop + chartHeight, axisPaint);
    }
}