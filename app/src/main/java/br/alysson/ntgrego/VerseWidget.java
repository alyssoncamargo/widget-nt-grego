package br.alysson.ntgrego;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class VerseWidget extends AppWidgetProvider {

    static final String ACTION_SHUFFLE = "br.alysson.ntgrego.SHUFFLE";

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        VerseRepo.Passage v = VerseRepo.today(ctx);
        for (int id : ids) mgr.updateAppWidget(id, build(ctx, v));
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String a = intent.getAction();
        if (ACTION_SHUFFLE.equals(a)) {
            VerseRepo.shuffleToday(ctx);
            updateAll(ctx);
            return;
        }
        if (Intent.ACTION_DATE_CHANGED.equals(a) || Intent.ACTION_TIME_CHANGED.equals(a)) {
            updateAll(ctx);
            return;
        }
        super.onReceive(ctx, intent);
    }

    static void updateAll(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, VerseWidget.class));
        VerseRepo.Passage v = VerseRepo.today(ctx);
        for (int id : ids) mgr.updateAppWidget(id, build(ctx, v));
    }

    private static RemoteViews build(Context ctx, VerseRepo.Passage v) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget);
        rv.setTextViewText(R.id.w_text, v.text);
        rv.setTextViewText(R.id.w_ref, v.reference());

        Intent open = new Intent(ctx, ReaderActivity.class)
                .putExtra(ReaderActivity.EXTRA_START, v.first.index)
                .putExtra(ReaderActivity.EXTRA_END, v.last.index)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent piOpen = PendingIntent.getActivity(ctx, 1, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.w_root, piOpen);

        Intent shuffle = new Intent(ctx, VerseWidget.class).setAction(ACTION_SHUFFLE);
        PendingIntent piShuffle = PendingIntent.getBroadcast(ctx, 2, shuffle,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.w_shuffle, piShuffle);
        return rv;
    }
}
