package app.usefoster.widget

import androidx.compose.runtime.Composable
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview

/**
 * Renders the widget design directly in Android Studio (debug builds only).
 *
 * Wide (320×160): calendar + checklist. Narrow (150×160): checklist only.
 * On device, SizeMode.Exact follows the launcher-allocated size — resize the
 * widget below ~220dp wide to see the half layout.
 *
 * @see androidx.glance.appwidget.preview
 */
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 160)
@Composable
fun HomeWidgetDesignPreviewWide() {
    HomeWidgetContent()
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 150, heightDp = 160)
@Composable
fun HomeWidgetDesignPreviewNarrow() {
    HomeWidgetContent()
}
