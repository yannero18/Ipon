package com.ipon.app.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Custom category iconography replacing emoji throughout the app.
 *
 * Why this exists: emoji render inconsistently across Android OEM skins
 * (Samsung, Pixel, OnePlus all ship different emoji fonts with different
 * proportions and colors that clash with the app's own palette), and they
 * can't be tinted to match category accent colors the way a real icon can.
 * These are simple, single-color glyphs designed to sit inside the same
 * squircle chips used everywhere else (see IponShapes.SquircleSm), tintable
 * with `Icon(imageVector = ..., tint = categoryColor)`.
 *
 * Deliberately simple silhouettes rather than detailed illustrations -- at
 * the 18-20dp sizes these render at inside a transaction row, fine detail
 * disappears anyway, so simple shapes read more clearly than ornate ones.
 */
object IponIcons {

    val Transpo: ImageVector by lazy {
        ImageVector.Builder(
            name = "Transpo", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3f, 15f)
                lineTo(3f, 11f)
                curveTo(3f, 9.9f, 3.9f, 9f, 5f, 9f)
                lineTo(16f, 9f)
                curveTo(17.1f, 9f, 18f, 9.9f, 18f, 11f)
                lineTo(20f, 11f)
                curveTo(20.6f, 11f, 21f, 11.4f, 21f, 12f)
                lineTo(21f, 15f)
                lineTo(19f, 15f)
                curveTo(19f, 13.3f, 17.7f, 12f, 16f, 12f)
                curveTo(14.3f, 12f, 13f, 13.3f, 13f, 15f)
                lineTo(8f, 15f)
                curveTo(8f, 13.3f, 6.7f, 12f, 5f, 12f)
                curveTo(3.3f, 12f, 2f, 13.3f, 2f, 15f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(5f, 17.5f)
                curveTo(6.4f, 17.5f, 7.5f, 16.4f, 7.5f, 15f)
                curveTo(7.5f, 13.6f, 6.4f, 12.5f, 5f, 12.5f)
                curveTo(3.6f, 12.5f, 2.5f, 13.6f, 2.5f, 15f)
                curveTo(2.5f, 16.4f, 3.6f, 17.5f, 5f, 17.5f)
                close()
                moveTo(16f, 17.5f)
                curveTo(17.4f, 17.5f, 18.5f, 16.4f, 18.5f, 15f)
                curveTo(18.5f, 13.6f, 17.4f, 12.5f, 16f, 12.5f)
                curveTo(14.6f, 12.5f, 13.5f, 13.6f, 13.5f, 15f)
                curveTo(13.5f, 16.4f, 14.6f, 17.5f, 16f, 17.5f)
                close()
            }
        }.build()
    }

    val Food: ImageVector by lazy {
        ImageVector.Builder(
            name = "Food", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(4f, 13f)
                lineTo(20f, 13f)
                curveTo(20f, 17.4f, 16.4f, 21f, 12f, 21f)
                curveTo(7.6f, 21f, 4f, 17.4f, 4f, 13f)
                close()
            }
        }.build()
    }

    val Bills: ImageVector by lazy {
        ImageVector.Builder(
            name = "Bills", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                curveTo(8.1f, 2f, 5f, 5.1f, 5f, 9f)
                curveTo(5f, 11.4f, 6.2f, 13.5f, 8f, 14.7f)
                lineTo(8f, 17f)
                curveTo(8f, 17.6f, 8.4f, 18f, 9f, 18f)
                lineTo(15f, 18f)
                curveTo(15.6f, 18f, 16f, 17.6f, 16f, 17f)
                lineTo(16f, 14.7f)
                curveTo(17.8f, 13.5f, 19f, 11.4f, 19f, 9f)
                curveTo(19f, 5.1f, 15.9f, 2f, 12f, 2f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(9f, 20f)
                lineTo(15f, 20f)
                lineTo(15f, 21f)
                curveTo(15f, 21.6f, 14.6f, 22f, 14f, 22f)
                lineTo(10f, 22f)
                curveTo(9.4f, 22f, 9f, 21.6f, 9f, 21f)
                close()
            }
        }.build()
    }

    val Groceries: ImageVector by lazy {
        ImageVector.Builder(
            name = "Groceries", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(5f, 9f)
                lineTo(19f, 9f)
                lineTo(18f, 20f)
                curveTo(17.9f, 21.1f, 17f, 22f, 15.9f, 22f)
                lineTo(8.1f, 22f)
                curveTo(7f, 22f, 6.1f, 21.1f, 6f, 20f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(3f, 7f)
                lineTo(21f, 7f)
                curveTo(21.6f, 7f, 22f, 7.4f, 22f, 8f)
                curveTo(22f, 8.6f, 21.6f, 9f, 21f, 9f)
                lineTo(3f, 9f)
                curveTo(2.4f, 9f, 2f, 8.6f, 2f, 8f)
                curveTo(2f, 7.4f, 2.4f, 7f, 3f, 7f)
                close()
            }
        }.build()
    }

    val Shopping: ImageVector by lazy {
        ImageVector.Builder(
            name = "Shopping", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 8f)
                lineTo(18f, 8f)
                lineTo(19f, 21f)
                curveTo(19.1f, 21.6f, 18.6f, 22f, 18f, 22f)
                lineTo(6f, 22f)
                curveTo(5.4f, 22f, 4.9f, 21.6f, 5f, 21f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(8.5f, 6.5f)
                curveTo(8.5f, 4.6f, 10.1f, 3f, 12f, 3f)
                curveTo(13.9f, 3f, 15.5f, 4.6f, 15.5f, 6.5f)
                lineTo(15.5f, 9f)
                lineTo(13.5f, 9f)
                lineTo(13.5f, 6.5f)
                curveTo(13.5f, 5.7f, 12.8f, 5f, 12f, 5f)
                curveTo(11.2f, 5f, 10.5f, 5.7f, 10.5f, 6.5f)
                lineTo(10.5f, 9f)
                lineTo(8.5f, 9f)
                close()
            }
        }.build()
    }

    val Health: ImageVector by lazy {
        ImageVector.Builder(
            name = "Health", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                curveTo(6.5f, 2f, 2f, 6.5f, 2f, 12f)
                curveTo(2f, 17.5f, 6.5f, 22f, 12f, 22f)
                curveTo(17.5f, 22f, 22f, 17.5f, 22f, 12f)
                curveTo(22f, 6.5f, 17.5f, 2f, 12f, 2f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(11f, 7f)
                lineTo(13f, 7f)
                lineTo(13f, 11f)
                lineTo(17f, 11f)
                lineTo(17f, 13f)
                lineTo(13f, 13f)
                lineTo(13f, 17f)
                lineTo(11f, 17f)
                lineTo(11f, 13f)
                lineTo(7f, 13f)
                lineTo(7f, 11f)
                lineTo(11f, 11f)
                close()
            }
        }.build()
    }

    val Entertainment: ImageVector by lazy {
        ImageVector.Builder(
            name = "Entertainment", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(5f, 4f)
                lineTo(19f, 4f)
                curveTo(20.1f, 4f, 21f, 4.9f, 21f, 6f)
                lineTo(21f, 18f)
                curveTo(21f, 19.1f, 20.1f, 20f, 19f, 20f)
                lineTo(5f, 20f)
                curveTo(3.9f, 20f, 3f, 19.1f, 3f, 18f)
                lineTo(3f, 6f)
                curveTo(3f, 4.9f, 3.9f, 4f, 5f, 4f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(10f, 8.5f)
                lineTo(16f, 12f)
                lineTo(10f, 15.5f)
                close()
            }
        }.build()
    }

    val Education: ImageVector by lazy {
        ImageVector.Builder(
            name = "Education", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 6f)
                curveTo(10.5f, 4.8f, 8.5f, 4f, 6f, 4f)
                curveTo(4.9f, 4f, 4f, 4.9f, 4f, 6f)
                lineTo(4f, 17f)
                curveTo(4f, 18.1f, 4.9f, 19f, 6f, 19f)
                curveTo(8.5f, 19f, 10.5f, 19.8f, 12f, 21f)
                curveTo(13.5f, 19.8f, 15.5f, 19f, 18f, 19f)
                curveTo(19.1f, 19f, 20f, 18.1f, 20f, 17f)
                lineTo(20f, 6f)
                curveTo(20f, 4.9f, 19.1f, 4f, 18f, 4f)
                curveTo(15.5f, 4f, 13.5f, 4.8f, 12f, 6f)
                close()
            }
        }.build()
    }

    val Utang: ImageVector by lazy {
        ImageVector.Builder(
            name = "Utang", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(2f, 12f)
                curveTo(2f, 10.3f, 3.3f, 9f, 5f, 9f)
                lineTo(9f, 9f)
                lineTo(9f, 15f)
                lineTo(5f, 15f)
                curveTo(3.3f, 15f, 2f, 13.7f, 2f, 12f)
                close()
                moveTo(22f, 12f)
                curveTo(22f, 10.3f, 20.7f, 9f, 19f, 9f)
                lineTo(15f, 9f)
                lineTo(15f, 15f)
                lineTo(19f, 15f)
                curveTo(20.7f, 15f, 22f, 13.7f, 22f, 12f)
                close()
            }
            path(fill = SolidColor(Color(0xFFF28C38))) {
                moveTo(9f, 9f)
                lineTo(15f, 9f)
                lineTo(15f, 15f)
                lineTo(9f, 15f)
                close()
            }
        }.build()
    }

    val Padala: ImageVector by lazy {
        ImageVector.Builder(
            name = "Padala", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(4f, 8f)
                lineTo(12f, 4f)
                lineTo(20f, 8f)
                lineTo(20f, 17f)
                lineTo(12f, 21f)
                lineTo(4f, 17f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 4f)
                lineTo(20f, 8f)
                lineTo(12f, 12f)
                lineTo(4f, 8f)
                close()
            }
        }.build()
    }

    val Salary: ImageVector by lazy {
        ImageVector.Builder(
            name = "Salary", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                curveTo(6.5f, 2f, 2f, 6.5f, 2f, 12f)
                curveTo(2f, 17.5f, 6.5f, 22f, 12f, 22f)
                curveTo(17.5f, 22f, 22f, 17.5f, 22f, 12f)
                curveTo(22f, 6.5f, 17.5f, 2f, 12f, 2f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(9.5f, 7f)
                lineTo(13f, 7f)
                curveTo(14.7f, 7f, 16f, 8.3f, 16f, 10f)
                curveTo(16f, 11.4f, 15f, 12.6f, 13.6f, 12.9f)
                lineTo(9.5f, 12.9f)
                lineTo(9.5f, 11f)
                lineTo(13f, 11f)
                curveTo(13.6f, 11f, 14f, 10.6f, 14f, 10f)
                curveTo(14f, 9.4f, 13.6f, 9f, 13f, 9f)
                lineTo(9.5f, 9f)
                close()
            }
        }.build()
    }

    val Government: ImageVector by lazy {
        ImageVector.Builder(
            name = "Government", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            // Simple columned-building silhouette (a government/municipal hall).
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                lineTo(21f, 7f)
                lineTo(21f, 9f)
                lineTo(3f, 9f)
                lineTo(3f, 7f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(4f, 10f)
                lineTo(6f, 10f)
                lineTo(6f, 19f)
                lineTo(4f, 19f)
                close()
                moveTo(9f, 10f)
                lineTo(11f, 10f)
                lineTo(11f, 19f)
                lineTo(9f, 19f)
                close()
                moveTo(13f, 10f)
                lineTo(15f, 10f)
                lineTo(15f, 19f)
                lineTo(13f, 19f)
                close()
                moveTo(18f, 10f)
                lineTo(20f, 10f)
                lineTo(20f, 19f)
                lineTo(18f, 19f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(3f, 20f)
                lineTo(21f, 20f)
                lineTo(21f, 22f)
                lineTo(3f, 22f)
                close()
            }
        }.build()
    }

    val Other: ImageVector by lazy {
        ImageVector.Builder(
            name = "Other", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20.6f, 11.6f)
                lineTo(12.4f, 3.4f)
                curveTo(12f, 3f, 11.5f, 2.8f, 11f, 2.8f)
                lineTo(5f, 2.8f)
                curveTo(3.9f, 2.8f, 3f, 3.7f, 3f, 4.8f)
                lineTo(3f, 10.8f)
                curveTo(3f, 11.3f, 3.2f, 11.8f, 3.6f, 12.2f)
                lineTo(11.8f, 20.4f)
                curveTo(12.6f, 21.2f, 13.9f, 21.2f, 14.7f, 20.4f)
                lineTo(20.6f, 14.5f)
                curveTo(21.4f, 13.7f, 21.4f, 12.4f, 20.6f, 11.6f)
                close()
            }
        }.build()
    }
}
