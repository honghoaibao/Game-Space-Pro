package com.gamespace.ui.icons.outlined

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Xem GsFilledIcons.kt - cung co che, style "Outlined" thay vi "Filled". */

val Icons.Outlined.Home: ImageVector
    get() {
        _home_outlined?.let { return it }
        _home_outlined = ImageVector.Builder(
            name = "Outlined.Home",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M12,5.69l5,4.5V18h-2v-6H9v6H7v-7.81l5,-4.5M12,3L2,12h3v8h6v-6h2v6h6v-8h3L12,3z"), fill = SolidColor(Color.Black))
        }.build()
        return _home_outlined!!
    }

val Icons.Outlined.AccessTime: ImageVector
    get() {
        _access_time_outlined?.let { return it }
        _access_time_outlined = ImageVector.Builder(
            name = "Outlined.AccessTime",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M11.99,2C6.47,2 2,6.48 2,12s4.47,10 9.99,10C17.52,22 22,17.52 22,12S17.52,2 11.99,2zM12,20c-4.42,0 -8,-3.58 -8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8zM12.5,7L11,7v6l5.25,3.15 0.75,-1.23 -4.5,-2.67z"), fill = SolidColor(Color.Black))
        }.build()
        return _access_time_outlined!!
    }

val Icons.Outlined.Person: ImageVector
    get() {
        _person_outlined?.let { return it }
        _person_outlined = ImageVector.Builder(
            name = "Outlined.Person",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M12,6c1.1,0 2,0.9 2,2s-0.9,2 -2,2 -2,-0.9 -2,-2 0.9,-2 2,-2m0,10c2.7,0 5.8,1.29 6,2L6,18c0.23,-0.72 3.31,-2 6,-2m0,-12C9.79,4 8,5.79 8,8s1.79,4 4,4 4,-1.79 4,-4 -1.79,-4 -4,-4zM12,14c-2.67,0 -8,1.34 -8,4v2h16v-2c0,-2.66 -5.33,-4 -8,-4z"), fill = SolidColor(Color.Black))
        }.build()
        return _person_outlined!!
    }

val Icons.Outlined.Star: ImageVector
    get() {
        _star_outlined?.let { return it }
        _star_outlined = ImageVector.Builder(
            name = "Outlined.Star",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M12,17.27L18.18,21l-1.64,-7.03L22,9.24l-7.19,-0.61L12,2L9.19,8.63L2,9.24l5.46,4.73L5.82,21L12,17.27z"), fill = SolidColor(Color.Black))
        }.build()
        return _star_outlined!!
    }

private var _home_outlined: ImageVector? = null
private var _access_time_outlined: ImageVector? = null
private var _person_outlined: ImageVector? = null
private var _star_outlined: ImageVector? = null
