package in.aicon.pocket;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.widget.TextView;

import java.math.BigInteger;

/** Shared visual tokens for Hisab's native, programmatic Android UI. */
final class HisabDesign {
    private HisabDesign() {}

    // 4dp rhythm, with named values used by layouts rather than ad-hoc gaps.
    static final int SPACE_1 = 4;
    static final int SPACE_2 = 8;
    static final int SPACE_3 = 12;
    static final int SPACE_4 = 16;
    static final int SPACE_5 = 20;
    static final int SPACE_6 = 24;
    static final int SPACE_7 = 32;

    static final int RADIUS_CONTROL = 14;
    static final int RADIUS_ROW = 16;
    static final int RADIUS_SURFACE = 18;
    static final int RADIUS_HERO = 22;
    static final int MIN_TOUCH_DP = 48;

    static final int TYPE_LABEL = 11;
    static final int TYPE_META = 12;
    static final int TYPE_BODY = 14;
    static final int TYPE_SECTION = 18;
    static final int TYPE_PAGE = 26;
    static final int TYPE_MONEY = 32;

    static final long MOTION_SHORT_MS = 140L;
    static final long MOTION_STANDARD_MS = 220L;

    static final class Palette {
        final int background;
        final int foreground;
        final int secondaryText;
        final int mutedText;
        final int surface;
        final int elevatedSurface;
        final int divider;
        final int accent;
        final int onAccent;
        final int accentSoft;
        final int heroSurface;
        final int income;
        final int expense;
        final int transfer;
        final int savings;
        final int warning;
        final int error;
        final int success;

        Palette(boolean dark, boolean pureBlack, boolean glass,
                int background, int foreground, int mutedText, int surface,
                int divider, int heroSurface, int accent, int onAccent) {
            this.background = glass || (dark && pureBlack) ? Color.BLACK : background;
            this.foreground = foreground;
            this.secondaryText = mutedText;
            this.mutedText = mutedText;
            this.surface = surface;
            this.elevatedSurface = glass ? 0xff11161b
                    : dark ? (pureBlack ? 0xff111715 : blend(surface, foreground, .035f))
                    : 0xfffcfdf9;
            this.divider = glass ? 0xff2a333b
                    : dark ? (pureBlack ? 0xff2c3631 : divider) : divider;
            this.accent = accent;
            this.onAccent = onAccent;
            this.accentSoft = blend(surface, accent, dark ? .20f : .12f);
            this.heroSurface = glass ? 0xff0b1116
                    : dark && pureBlack ? 0xff0d1713 : heroSurface;
            this.income = dark ? 0xff8bd0ad : 0xff236c51;
            this.expense = dark ? 0xfff0a17e : 0xffa75332;
            this.transfer = dark ? 0xffa9c3e2 : 0xff46698f;
            this.savings = dark ? 0xffd4bd86 : 0xff80683a;
            this.warning = dark ? 0xffffd078 : 0xff815800;
            this.error = dark ? 0xffffb4ab : 0xffb3261e;
            this.success = this.income;
        }
    }

    static Palette palette(boolean dark, boolean pureBlack, boolean glass,
                           int background, int foreground, int mutedText,
                           int surface, int divider, int heroSurface,
                           int accent, int onAccent) {
        return new Palette(dark, pureBlack, glass, background, foreground,
                mutedText, surface, divider, heroSurface, accent, onAccent);
    }

    static int progressPercent(long value, long limit) {
        if (limit <= 0 || value <= 0) return 0;
        if (value >= limit) return 100;
        return BigInteger.valueOf(value).multiply(BigInteger.valueOf(100))
                .divide(BigInteger.valueOf(limit)).intValue();
    }

    static Drawable inputBackground(Context context, int surface, int outline, int accent) {
        float density = context.getResources().getDisplayMetrics().density;
        StateListDrawable states = new StateListDrawable();
        GradientDrawable focused = rounded(surface, Math.round(14 * density));
        focused.setStroke(Math.max(1, Math.round(2 * density)), accent);
        GradientDrawable normal = rounded(surface, Math.round(14 * density));
        normal.setStroke(Math.max(1, Math.round(density)), outline);
        states.addState(new int[]{android.R.attr.state_focused}, focused);
        states.addState(new int[]{}, normal);
        return states;
    }

    static void tabularNumbers(TextView view) {
        if (Build.VERSION.SDK_INT >= 21) view.setFontFeatureSettings("'tnum'");
    }

    static boolean animationsEnabled() {
        return Build.VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled();
    }

    private static GradientDrawable rounded(int color, int radiusPx) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radiusPx);
        return drawable;
    }

    private static int blend(int base, int tint, float amount) {
        float f = Math.max(0f, Math.min(1f, amount));
        return Color.rgb(
                Math.round(Color.red(base) * (1f - f) + Color.red(tint) * f),
                Math.round(Color.green(base) * (1f - f) + Color.green(tint) * f),
                Math.round(Color.blue(base) * (1f - f) + Color.blue(tint) * f));
    }
}
