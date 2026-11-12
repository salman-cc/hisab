package in.aicon.pocket;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;

import java.util.function.LongFunction;

/** On-device charts. Integer minor-unit amounts stay exact; float values are used only to draw geometry. */
final class LedgerChart extends View {
    String emptyLabel = "No expenses through today";
    String expenseLabel = "Expenses";
    String dayLabel = "Day";
    String summary = "";
    String[] sliceLabels = new String[0];
    final long[] values;
    final int[] colours;
    final int foreground;
    final int muted;
    final int accent;
    final int surface;
    final boolean donut;
    final int throughDay;
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    LongFunction<String> formatter = Ledger::number;
    int selectedIndex = -1;

    private float sp(float size) {
        return android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_SP, size,
                getResources().getDisplayMetrics());
    }

    LedgerChart(Context context, long[] values, int[] colours, boolean donut,
                int throughDay, int foreground, int muted, int accent) {
        this(context, values, colours, donut, throughDay, foreground, muted, accent, Color.WHITE);
    }

    LedgerChart(Context context, long[] values, int[] colours, boolean donut,
                int throughDay, int foreground, int muted, int accent, int surface) {
        super(context);
        this.values = values.clone();
        this.colours = colours.clone();
        this.donut = donut;
        this.throughDay = throughDay;
        this.foreground = foreground;
        this.muted = muted;
        this.accent = accent;
        this.surface = surface;
        setClickable(true);
        setFocusable(true);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    void labels(String empty, String expenses) {
        emptyLabel = empty;
        expenseLabel = expenses;
        invalidate();
    }

    void interaction(String day, LongFunction<String> amountFormatter, String[] labels, String summary) {
        dayLabel = day;
        if (amountFormatter != null) formatter = amountFormatter;
        sliceLabels = labels == null ? new String[0] : labels.clone();
        this.summary = summary == null ? "" : summary;
        setContentDescription(this.summary);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float d = getResources().getDisplayMetrics().density;
        float width = getWidth();
        float height = getHeight();
        long total = 0;
        for (long value : values) total = Math.addExact(total, value);
        p.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        if (total == 0) {
            p.setColor(muted);
            p.setTextSize(sp(13));
            String fitted = TextUtils.ellipsize(emptyLabel, new android.text.TextPaint(p),
                    Math.max(0, width - 24 * d), TextUtils.TruncateAt.END).toString();
            canvas.drawText(fitted, 12 * d, height / 2, p);
            return;
        }
        if (donut) drawDonut(canvas, width, height, d, total);
        else drawCumulative(canvas, width, height, d);
    }

    private void drawDonut(Canvas canvas, float width, float height, float d, long total) {
        float size = Math.min(width, height) - 20 * d;
        float cx = width / 2;
        float cy = height / 2;
        RectF ring = new RectF(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2);
        ring.inset(15 * d, 15 * d);
        float stroke = 24 * d;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.BUTT);
        p.setStrokeWidth(stroke);
        float start = -90;
        for (int i = 0; i < values.length; i++) {
            float sweep = (float) (values[i] * 360.0 / (double) total);
            p.setColor(colours.length == 0 ? accent : colours[i % colours.length]);
            p.setStrokeWidth(selectedIndex == i ? 30 * d : stroke);
            canvas.drawArc(ring, start, Math.max(0, sweep - 2), false, p);
            start += sweep;
        }
        p.setStyle(Paint.Style.FILL);
        float innerWidth = Math.max(0, ring.width() - stroke * 2);
        String headline = expenseLabel;
        String amount;
        if (selectedIndex >= 0 && selectedIndex < values.length) {
            if (selectedIndex < sliceLabels.length && !sliceLabels[selectedIndex].isEmpty())
                headline = sliceLabels[selectedIndex];
            amount = formatter.apply(values[selectedIndex]);
        } else {
            amount = formatter.apply(total);
        }
        p.setTextAlign(Paint.Align.CENTER);
        float headlineSize = sp(11);
        float amountSize = sp(13);
        float baselineGap = Math.max(18 * d, (headlineSize + amountSize) * .55f);
        p.setColor(muted);
        p.setTextSize(headlineSize);
        String fittedHeadline = TextUtils.ellipsize(headline, new android.text.TextPaint(p), innerWidth, TextUtils.TruncateAt.END).toString();
        canvas.drawText(fittedHeadline, cx, cy - baselineGap / 2, p);
        p.setColor(foreground);
        p.setTextSize(amountSize);
        String fittedAmount = TextUtils.ellipsize(amount, new android.text.TextPaint(p), innerWidth, TextUtils.TruncateAt.END).toString();
        canvas.drawText(fittedAmount, cx, cy + baselineGap / 2, p);
        p.setTextAlign(Paint.Align.LEFT);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawCumulative(Canvas canvas, float width, float height, float d) {
        float left = 12 * d;
        float right = width - 12 * d;
        float top = Math.max(15 * d, sp(11) + 4 * d);
        float bottom = height - Math.max(30 * d, sp(11) + 8 * d);
        long cumulative = 0;
        long maximum = 0;
        for (long value : values) {
            cumulative = Math.addExact(cumulative, value);
            maximum = Math.max(maximum, cumulative);
        }
        maximum = Math.max(maximum, 1);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1, d));
        p.setColor((muted & 0x00ffffff) | 0x30000000);
        for (int i = 0; i < 3; i++) {
            float y = top + (bottom - top) * i / 2;
            canvas.drawLine(left, y, right, y, p);
        }

        int end = Math.min(values.length, Math.max(1, throughDay));
        Path line = new Path();
        Path area = new Path();
        area.moveTo(left, bottom);
        cumulative = 0;
        float lastX = left;
        float lastY = bottom;
        for (int i = 0; i < end; i++) {
            cumulative = Math.addExact(cumulative, values[i]);
            float x = left + (right - left) * i / Math.max(1, values.length - 1);
            float y = bottom - (bottom - top) * (float) ((double) cumulative / maximum);
            if (i == 0) {
                line.moveTo(x, y);
                area.lineTo(x, y);
            } else {
                line.lineTo(x, y);
                area.lineTo(x, y);
            }
            lastX = x;
            lastY = y;
        }
        area.lineTo(lastX, bottom);
        area.close();
        int tint = (accent & 0x00ffffff) | 0x26000000;
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0, top, 0, bottom, tint, 0x00000000, Shader.TileMode.CLAMP));
        canvas.drawPath(area, p);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(accent);
        p.setStrokeWidth(2.5f * d);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        canvas.drawPath(line, p);

        int marker = selectedIndex >= 0 && selectedIndex < end ? selectedIndex : end - 1;
        long markerValue = 0;
        for (int i = 0; i <= marker; i++) markerValue = Math.addExact(markerValue, values[i]);
        float markerX = left + (right - left) * marker / Math.max(1, values.length - 1);
        float markerY = bottom - (bottom - top) * (float) ((double) markerValue / maximum);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(d);
        p.setColor((muted & 0x00ffffff) | 0x65000000);
        if (selectedIndex >= 0 && selectedIndex < end) canvas.drawLine(markerX, top, markerX, bottom, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(accent);
        canvas.drawCircle(markerX, markerY, selectedIndex >= 0 ? 5 * d : 4 * d, p);
        p.setColor(muted);
        p.setTextSize(sp(11));
        Paint.FontMetrics axisMetrics = p.getFontMetrics();
        float axisBaseline = height - Math.max(3 * d, axisMetrics.descent + 2 * d);
        canvas.drawText("1", left, axisBaseline, p);
        p.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(String.valueOf(values.length), right, axisBaseline, p);
        p.setTextAlign(Paint.Align.LEFT);

        if (selectedIndex >= 0 && selectedIndex < end) drawTooltip(canvas, width, d, marker, markerValue);
    }

    private void drawTooltip(Canvas canvas, float width, float d, int day, long value) {
        String label = dayLabel + " " + (day + 1) + " · " + formatter.apply(value);
        p.setTextSize(sp(11));
        p.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        Paint.FontMetrics metrics = p.getFontMetrics();
        float padding = 10 * d;
        float boxWidth = Math.min(Math.max(1, width - 16 * d), p.measureText(label) + padding * 2);
        float boxHeight = Math.max(25 * d, metrics.descent - metrics.ascent + 6 * d);
        RectF bounds = new RectF(8 * d, 3 * d, 8 * d + boxWidth, 3 * d + boxHeight);
        p.setStyle(Paint.Style.FILL);
        p.setColor(surface);
        canvas.drawRoundRect(bounds, 8 * d, 8 * d, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1, d));
        p.setColor((accent & 0x00ffffff) | 0x88000000);
        canvas.drawRoundRect(bounds, 8 * d, 8 * d, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(foreground);
        p.setTextAlign(Paint.Align.LEFT);
        String fitted = TextUtils.ellipsize(label, new android.text.TextPaint(p), boxWidth - padding * 2, TextUtils.TruncateAt.END).toString();
        canvas.drawText(fitted, bounds.left + padding,
                bounds.centerY() - (metrics.ascent + metrics.descent) / 2f, p);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            if (donut) selectSlice(event.getX(), event.getY());
            else selectDay(event.getX());
            if (selectedIndex >= 0) {
                String selected;
                if (donut) {
                    String name = selectedIndex < sliceLabels.length ? sliceLabels[selectedIndex] : expenseLabel;
                    selected = name + ": " + formatter.apply(values[selectedIndex]);
                } else {
                    long amount = 0;
                    for (int i = 0; i <= selectedIndex; i++) amount = Math.addExact(amount, values[i]);
                    selected = dayLabel + " " + (selectedIndex + 1) + ": " + formatter.apply(amount);
                }
                setContentDescription((summary.isEmpty() ? expenseLabel : summary) + ". " + selected);
            }
            invalidate();
            performClick();
            return true;
        }
        return event.getActionMasked() == MotionEvent.ACTION_DOWN || super.onTouchEvent(event);
    }

    private void selectDay(float x) {
        if (values.length == 0 || throughDay <= 0) return;
        float d = getResources().getDisplayMetrics().density;
        float left = 12 * d;
        float right = getWidth() - 12 * d;
        float fraction = Math.max(0, Math.min(1, (x - left) / Math.max(1, right - left)));
        selectedIndex = Math.min(Math.min(values.length, throughDay) - 1,
                Math.max(0, Math.round(fraction * Math.max(1, values.length - 1))));
    }

    private void selectSlice(float x, float y) {
        long total = 0;
        for (long value : values) total = Math.addExact(total, value);
        if (total <= 0 || values.length == 0) return;
        float d = getResources().getDisplayMetrics().density;
        float size = Math.min(getWidth(), getHeight()) - 20 * d;
        float radius = (float) Math.hypot(x - getWidth() / 2f, y - getHeight() / 2f);
        if (radius < size * .27f || radius > size * .52f) return;
        double angle = (Math.toDegrees(Math.atan2(y - getHeight() / 2f, x - getWidth() / 2f)) + 450) % 360;
        double cursor = 0;
        for (int i = 0; i < values.length; i++) {
            cursor += values[i] * 360.0 / total;
            if (angle <= cursor || i == values.length - 1) {
                selectedIndex = i;
                return;
            }
        }
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }
}
