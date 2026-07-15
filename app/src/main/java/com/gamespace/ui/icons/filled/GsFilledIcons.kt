package com.gamespace.ui.icons.filled

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Icon "Filled" tu build tu path data GOC (lay tu google/material-design-icons, cung repo la
 * nguon cua androidx material-icons-extended) - THAY THE hoan toan dependency
 * material-icons-extended (rat nang, Google khuyen nghi khong include truc tiep trong app
 * production). Cach dung KHONG doi o noi goi: van viet Icons.Filled.TenIcon nhu cu, chi doi
 * import sang package nay. File sinh tu dong bang script - xem TASK_BACKLOG.md Phien 11.
 */

val Icons.Filled.Assessment: ImageVector
    get() {
        _assessment?.let { return it }
        _assessment = ImageVector.Builder(
            name = "Filled.Assessment",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M19,3L5,3c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2L21,5c0,-1.1 -0.9,-2 -2,-2zM9,17L7,17v-7h2v7zM13,17h-2L11,7h2v10zM17,17h-2v-4h2v4z"), fill = SolidColor(Color.Black))
        }.build()
        return _assessment!!
    }

val Icons.Filled.CheckCircle: ImageVector
    get() {
        _check_circle?.let { return it }
        _check_circle = ImageVector.Builder(
            name = "Filled.CheckCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM10,17l-5,-5 1.41,-1.41L10,14.17l7.59,-7.59L19,8l-9,9z"), fill = SolidColor(Color.Black))
        }.build()
        return _check_circle!!
    }

val Icons.Filled.Delete: ImageVector
    get() {
        _delete?.let { return it }
        _delete = ImageVector.Builder(
            name = "Filled.Delete",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6v12zM19,4h-3.5l-1,-1h-5l-1,1H5v2h14V4z"), fill = SolidColor(Color.Black))
        }.build()
        return _delete!!
    }

val Icons.Filled.HelpOutline: ImageVector
    get() {
        _help_outline?.let { return it }
        _help_outline = ImageVector.Builder(
            name = "Filled.HelpOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M11,18h2v-2h-2v2zM12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,20c-4.41,0 -8,-3.59 -8,-8s3.59,-8 8,-8 8,3.59 8,8 -3.59,8 -8,8zM12,6c-2.21,0 -4,1.79 -4,4h2c0,-1.1 0.9,-2 2,-2s2,0.9 2,2c0,2 -3,1.75 -3,5h2c0,-2.25 3,-2.5 3,-5 0,-2.21 -1.79,-4 -4,-4z"), fill = SolidColor(Color.Black))
        }.build()
        return _help_outline!!
    }

val Icons.Filled.History: ImageVector
    get() {
        _history?.let { return it }
        _history = ImageVector.Builder(
            name = "Filled.History",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M13,3c-4.97,0 -9,4.03 -9,9L1,12l3.89,3.89 0.07,0.14L9,12L6,12c0,-3.87 3.13,-7 7,-7s7,3.13 7,7 -3.13,7 -7,7c-1.93,0 -3.68,-0.79 -4.94,-2.06l-1.42,1.42C8.27,19.99 10.51,21 13,21c4.97,0 9,-4.03 9,-9s-4.03,-9 -9,-9zM12,8v5l4.28,2.54 0.72,-1.21 -3.5,-2.08L13.5,8L12,8z"), fill = SolidColor(Color.Black))
        }.build()
        return _history!!
    }

val Icons.Filled.Home: ImageVector
    get() {
        _home?.let { return it }
        _home = ImageVector.Builder(
            name = "Filled.Home",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M10,20v-6h4v6h5v-8h3L12,3 2,12h3v8z"), fill = SolidColor(Color.Black))
        }.build()
        return _home!!
    }

val Icons.Filled.Lock: ImageVector
    get() {
        _lock?.let { return it }
        _lock = ImageVector.Builder(
            name = "Filled.Lock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M18,8h-1L17,6c0,-2.76 -2.24,-5 -5,-5S7,3.24 7,6v2L6,8c-1.1,0 -2,0.9 -2,2v10c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2L20,10c0,-1.1 -0.9,-2 -2,-2zM12,17c-1.1,0 -2,-0.9 -2,-2s0.9,-2 2,-2 2,0.9 2,2 -0.9,2 -2,2zM15.1,8L8.9,8L8.9,6c0,-1.71 1.39,-3.1 3.1,-3.1 1.71,0 3.1,1.39 3.1,3.1v2z"), fill = SolidColor(Color.Black))
        }.build()
        return _lock!!
    }

val Icons.Filled.LockOpen: ImageVector
    get() {
        _lock_open?.let { return it }
        _lock_open = ImageVector.Builder(
            name = "Filled.LockOpen",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M12,17c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2 -2,0.9 -2,2 0.9,2 2,2zM18,8h-1L17,6c0,-2.76 -2.24,-5 -5,-5S7,3.24 7,6h1.9c0,-1.71 1.39,-3.1 3.1,-3.1 1.71,0 3.1,1.39 3.1,3.1v2L6,8c-1.1,0 -2,0.9 -2,2v10c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2L20,10c0,-1.1 -0.9,-2 -2,-2zM18,20L6,20L6,10h12v10z"), fill = SolidColor(Color.Black))
        }.build()
        return _lock_open!!
    }

val Icons.Filled.Opacity: ImageVector
    get() {
        _opacity?.let { return it }
        _opacity = ImageVector.Builder(
            name = "Filled.Opacity",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M17.66,8L12,2.35 6.34,8C4.78,9.56 4,11.64 4,13.64s0.78,4.11 2.34,5.67 3.61,2.35 5.66,2.35 4.1,-0.79 5.66,-2.35S20,15.64 20,13.64 19.22,9.56 17.66,8zM6,14c0.01,-2 0.62,-3.27 1.76,-4.4L12,5.27l4.24,4.38C17.38,10.77 17.99,12 18,14H6z"), fill = SolidColor(Color.Black))
        }.build()
        return _opacity!!
    }

val Icons.Filled.OpenInNew: ImageVector
    get() {
        _open_in_new?.let { return it }
        _open_in_new = ImageVector.Builder(
            name = "Filled.OpenInNew",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M19,19H5V5h7V3H5c-1.11,0 -2,0.9 -2,2v14c0,1.1 0.89,2 2,2h14c1.1,0 2,-0.9 2,-2v-7h-2v7zM14,3v2h3.59l-9.83,9.83 1.41,1.41L19,6.41V10h2V3h-7z"), fill = SolidColor(Color.Black))
        }.build()
        return _open_in_new!!
    }

val Icons.Filled.PictureInPicture: ImageVector
    get() {
        _picture_in_picture?.let { return it }
        _picture_in_picture = ImageVector.Builder(
            name = "Filled.PictureInPicture",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M19,7h-8v6h8L19,7zM21,3L3,3c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,1.98 2,1.98h18c1.1,0 2,-0.88 2,-1.98L23,5c0,-1.1 -0.9,-2 -2,-2zM21,19.01L3,19.01L3,4.98h18v14.03z"), fill = SolidColor(Color.Black))
        }.build()
        return _picture_in_picture!!
    }

val Icons.Filled.Search: ImageVector
    get() {
        _search?.let { return it }
        _search = ImageVector.Builder(
            name = "Filled.Search",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z"), fill = SolidColor(Color.Black))
        }.build()
        return _search!!
    }

val Icons.Filled.SwapHoriz: ImageVector
    get() {
        _swap_horiz?.let { return it }
        _swap_horiz = ImageVector.Builder(
            name = "Filled.SwapHoriz",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M6.99,11L3,15l3.99,4v-3H14v-2H6.99v-3zM21,9l-3.99,-4v3H10v2h7.01v3L21,9z"), fill = SolidColor(Color.Black))
        }.build()
        return _swap_horiz!!
    }

val Icons.Filled.TouchApp: ImageVector
    get() {
        _touch_app?.let { return it }
        _touch_app = ImageVector.Builder(
            name = "Filled.TouchApp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M9,11.24V7.5C9,6.12 10.12,5 11.5,5S14,6.12 14,7.5v3.74c1.21,-0.81 2,-2.18 2,-3.74C16,5.01 13.99,3 11.5,3S7,5.01 7,7.5C7,9.06 7.79,10.43 9,11.24zM18.84,15.87l-4.54,-2.26c-0.17,-0.07 -0.35,-0.11 -0.54,-0.11H13v-6C13,6.67 12.33,6 11.5,6S10,6.67 10,7.5v10.74c-3.6,-0.76 -3.54,-0.75 -3.67,-0.75c-0.31,0 -0.59,0.13 -0.79,0.33l-0.79,0.8l4.94,4.94C9.96,23.83 10.34,24 10.75,24h6.79c0.75,0 1.33,-0.55 1.44,-1.28l0.75,-5.27c0.01,-0.07 0.02,-0.14 0.02,-0.2C19.75,16.63 19.37,16.09 18.84,15.87z"), fill = SolidColor(Color.Black))
        }.build()
        return _touch_app!!
    }

val Icons.Filled.ErrorOutline: ImageVector
    get() {
        _error_outline?.let { return it }
        _error_outline = ImageVector.Builder(
            name = "Filled.ErrorOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M11,15h2v2h-2zM11,7h2v6h-2zM11.99,2C6.47,2 2,6.48 2,12s4.47,10 9.99,10C17.52,22 22,17.52 22,12S17.52,2 11.99,2zM12,20c-4.42,0 -8,-3.58 -8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8z"), fill = SolidColor(Color.Black))
        }.build()
        return _error_outline!!
    }

val Icons.Filled.WarningAmber: ImageVector
    get() {
        _warning_amber?.let { return it }
        _warning_amber = ImageVector.Builder(
            name = "Filled.WarningAmber",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M12,5.99L19.53,19H4.47L12,5.99M12,2L1,21h22L12,2L12,2z"), fill = SolidColor(Color.Black))
            addPath(pathData = addPathNodes("M13,16l-2,0l0,2l2,0z"), fill = SolidColor(Color.Black))
            addPath(pathData = addPathNodes("M13,10l-2,0l0,5l2,0z"), fill = SolidColor(Color.Black))
        }.build()
        return _warning_amber!!
    }

val Icons.Filled.Games: ImageVector
    get() {
        _games?.let { return it }
        _games = ImageVector.Builder(
            name = "Filled.Games",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M15,7.5V2H9v5.5l3,3 3,-3zM7.5,9H2v6h5.5l3,-3 -3,-3zM9,16.5V22h6v-5.5l-3,-3 -3,3zM16.5,9l-3,3 3,3H22V9h-5.5z"), fill = SolidColor(Color.Black))
        }.build()
        return _games!!
    }

val Icons.Filled.PlayArrow: ImageVector
    get() {
        _play_arrow?.let { return it }
        _play_arrow = ImageVector.Builder(
            name = "Filled.PlayArrow",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M8,5v14l11,-7z"), fill = SolidColor(Color.Black))
        }.build()
        return _play_arrow!!
    }

val Icons.Filled.PlayCircle: ImageVector
    get() {
        _play_circle?.let { return it }
        _play_circle = ImageVector.Builder(
            name = "Filled.PlayCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10s10,-4.48 10,-10S17.52,2 12,2zM9.5,16.5v-9l7,4.5L9.5,16.5z"), fill = SolidColor(Color.Black))
        }.build()
        return _play_circle!!
    }

val Icons.Filled.Speed: ImageVector
    get() {
        _speed?.let { return it }
        _speed = ImageVector.Builder(
            name = "Filled.Speed",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M20.38,8.57l-1.23,1.85a8,8 0,0 1,-0.22 7.58L5.07,18A8,8 0,0 1,15.58 6.85l1.85,-1.23A10,10 0,0 0,3.35 19a2,2 0,0 0,1.72 1h13.85a2,2 0,0 0,1.74 -1,10 10,0 0,0 -0.27,-10.44zM10.59,15.41a2,2 0,0 0,2.83 0l5.66,-8.49 -8.49,5.66a2,2 0,0 0,0 2.83z"), fill = SolidColor(Color.Black))
        }.build()
        return _speed!!
    }

val Icons.Filled.StopCircle: ImageVector
    get() {
        _stop_circle?.let { return it }
        _stop_circle = ImageVector.Builder(
            name = "Filled.StopCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M12,2C6.48,2 2,6.48 2,12c0,5.52 4.48,10 10,10s10,-4.48 10,-10C22,6.48 17.52,2 12,2zM16,16H8V8h8V16z"), fill = SolidColor(Color.Black))
        }.build()
        return _stop_circle!!
    }

val Icons.Filled.Chat: ImageVector
    get() {
        _chat?.let { return it }
        _chat = ImageVector.Builder(
            name = "Filled.Chat",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M20,2L4,2c-1.1,0 -1.99,0.9 -1.99,2L2,22l4,-4h14c1.1,0 2,-0.9 2,-2L22,4c0,-1.1 -0.9,-2 -2,-2zM6,9h12v2L6,11L6,9zM14,14L6,14v-2h8v2zM18,8L6,8L6,6h12v2z"), fill = SolidColor(Color.Black))
        }.build()
        return _chat!!
    }

val Icons.Filled.Add: ImageVector
    get() {
        _add?.let { return it }
        _add = ImageVector.Builder(
            name = "Filled.Add",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"), fill = SolidColor(Color.Black))
        }.build()
        return _add!!
    }

val Icons.Filled.Bolt: ImageVector
    get() {
        _bolt?.let { return it }
        _bolt = ImageVector.Builder(
            name = "Filled.Bolt",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M11,21h-1l1,-7H7.5c-0.58,0 -0.57,-0.32 -0.38,-0.66 0.19,-0.34 0.05,-0.08 0.07,-0.12C8.48,10.94 10.42,7.54 13,3h1l-1,7h3.5c0.49,0 0.56,0.33 0.47,0.51l-0.07,0.15C12.96,17.55 11,21 11,21z"), fill = SolidColor(Color.Black))
        }.build()
        return _bolt!!
    }

val Icons.Filled.AccessTime: ImageVector
    get() {
        _access_time?.let { return it }
        _access_time = ImageVector.Builder(
            name = "Filled.AccessTime",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M11.99,2C6.47,2 2,6.48 2,12s4.47,10 9.99,10C17.52,22 22,17.52 22,12S17.52,2 11.99,2zM12,20c-4.42,0 -8,-3.58 -8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8z"), fill = SolidColor(Color.Black))
            addPath(pathData = addPathNodes("M12.5,7H11v6l5.25,3.15 0.75,-1.23 -4.5,-2.67z"), fill = SolidColor(Color.Black))
        }.build()
        return _access_time!!
    }

val Icons.Filled.BatterySaver: ImageVector
    get() {
        _battery_saver?.let { return it }
        _battery_saver = ImageVector.Builder(
            name = "Filled.BatterySaver",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M16,4h-2V2h-4v2H8C7.45,4 7,4.45 7,5v16c0,0.55 0.45,1 1,1h8c0.55,0 1,-0.45 1,-1V5C17,4.45 16.55,4 16,4zM15,14h-2v2h-2v-2H9v-2h2v-2h2v2h2V14z"), fill = SolidColor(Color.Black))
        }.build()
        return _battery_saver!!
    }

val Icons.Filled.Memory: ImageVector
    get() {
        _memory?.let { return it }
        _memory = ImageVector.Builder(
            name = "Filled.Memory",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M15,9L9,9v6h6L15,9zM13,13h-2v-2h2v2zM21,11L21,9h-2L19,7c0,-1.1 -0.9,-2 -2,-2h-2L15,3h-2v2h-2L11,3L9,3v2L7,5c-1.1,0 -2,0.9 -2,2v2L3,9v2h2v2L3,13v2h2v2c0,1.1 0.9,2 2,2h2v2h2v-2h2v2h2v-2h2c1.1,0 2,-0.9 2,-2v-2h2v-2h-2v-2h2zM17,17L7,17L7,7h10v10z"), fill = SolidColor(Color.Black))
        }.build()
        return _memory!!
    }

val Icons.Filled.PhoneAndroid: ImageVector
    get() {
        _phone_android?.let { return it }
        _phone_android = ImageVector.Builder(
            name = "Filled.PhoneAndroid",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M16,1L8,1C6.34,1 5,2.34 5,4v16c0,1.66 1.34,3 3,3h8c1.66,0 3,-1.34 3,-3L19,4c0,-1.66 -1.34,-3 -3,-3zM14,21h-4v-1h4v1zM17.25,18L6.75,18L6.75,4h10.5v14z"), fill = SolidColor(Color.Black))
        }.build()
        return _phone_android!!
    }

val Icons.Filled.MusicNote: ImageVector
    get() {
        _music_note?.let { return it }
        _music_note = ImageVector.Builder(
            name = "Filled.MusicNote",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M12,3v10.55c-0.59,-0.34 -1.27,-0.55 -2,-0.55 -2.21,0 -4,1.79 -4,4s1.79,4 4,4 4,-1.79 4,-4V7h4V3h-6z"), fill = SolidColor(Color.Black))
        }.build()
        return _music_note!!
    }

val Icons.Filled.Timer: ImageVector
    get() {
        _timer?.let { return it }
        _timer = ImageVector.Builder(
            name = "Filled.Timer",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M9,1h6v2h-6z"), fill = SolidColor(Color.Black))
            addPath(pathData = addPathNodes("M19.03,7.39l1.42,-1.42c-0.43,-0.51 -0.9,-0.99 -1.41,-1.41l-1.42,1.42C16.07,4.74 14.12,4 12,4c-4.97,0 -9,4.03 -9,9c0,4.97 4.02,9 9,9s9,-4.03 9,-9C21,10.88 20.26,8.93 19.03,7.39zM13,14h-2V8h2V14z"), fill = SolidColor(Color.Black))
        }.build()
        return _timer!!
    }

val Icons.Filled.Tune: ImageVector
    get() {
        _tune?.let { return it }
        _tune = ImageVector.Builder(
            name = "Filled.Tune",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M3,17v2h6v-2L3,17zM3,5v2h10L13,5L3,5zM13,21v-2h8v-2h-8v-2h-2v6h2zM7,9v2L3,11v2h4v2h2L9,9L7,9zM21,13v-2L11,11v2h10zM15,9h2L17,7h4L21,5h-4L17,3h-2v6z"), fill = SolidColor(Color.Black))
        }.build()
        return _tune!!
    }

val Icons.Filled.CleaningServices: ImageVector
    get() {
        _cleaning_services?.let { return it }
        _cleaning_services = ImageVector.Builder(
            name = "Filled.CleaningServices",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M16,11h-1V3c0,-1.1 -0.9,-2 -2,-2h-2C9.9,1 9,1.9 9,3v8H8c-2.76,0 -5,2.24 -5,5v7h18v-7C21,13.24 18.76,11 16,11zM19,21h-2v-3c0,-0.55 -0.45,-1 -1,-1s-1,0.45 -1,1v3h-2v-3c0,-0.55 -0.45,-1 -1,-1s-1,0.45 -1,1v3H9v-3c0,-0.55 -0.45,-1 -1,-1s-1,0.45 -1,1v3H5v-5c0,-1.65 1.35,-3 3,-3h8c1.65,0 3,1.35 3,3V21z"), fill = SolidColor(Color.Black))
        }.build()
        return _cleaning_services!!
    }

val Icons.Filled.ArrowBack: ImageVector
    get() {
        _arrow_back?.let { return it }
        _arrow_back = ImageVector.Builder(
            name = "Filled.ArrowBack",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z"), fill = SolidColor(Color.Black))
        }.build()
        return _arrow_back!!
    }

val Icons.Filled.Close: ImageVector
    get() {
        _close?.let { return it }
        _close = ImageVector.Builder(
            name = "Filled.Close",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z"), fill = SolidColor(Color.Black))
        }.build()
        return _close!!
    }

val Icons.Filled.UnfoldLess: ImageVector
    get() {
        _unfold_less?.let { return it }
        _unfold_less = ImageVector.Builder(
            name = "Filled.UnfoldLess",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M7.41,18.59L8.83,20 12,16.83 15.17,20l1.41,-1.41L12,14l-4.59,4.59zM16.59,5.41L15.17,4 12,7.17 8.83,4 7.41,5.41 12,10l4.59,-4.59z"), fill = SolidColor(Color.Black))
        }.build()
        return _unfold_less!!
    }

val Icons.Filled.EmojiEvents: ImageVector
    get() {
        _emoji_events?.let { return it }
        _emoji_events = ImageVector.Builder(
            name = "Filled.EmojiEvents",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M19,5h-2V3H7v2H5C3.9,5 3,5.9 3,7v1c0,2.55 1.92,4.63 4.39,4.94c0.63,1.5 1.98,2.63 3.61,2.96V19H7v2h10v-2h-4v-3.1c1.63,-0.33 2.98,-1.46 3.61,-2.96C19.08,12.63 21,10.55 21,8V7C21,5.9 20.1,5 19,5zM5,8V7h2v3.82C5.84,10.4 5,9.3 5,8zM19,8c0,1.3 -0.84,2.4 -2,2.82V7h2V8z"), fill = SolidColor(Color.Black))
        }.build()
        return _emoji_events!!
    }

val Icons.Filled.NotificationsOff: ImageVector
    get() {
        _notifications_off?.let { return it }
        _notifications_off = ImageVector.Builder(
            name = "Filled.NotificationsOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M20,18.69L7.84,6.14 5.27,3.49 4,4.76l2.8,2.8v0.01c-0.52,0.99 -0.8,2.16 -0.8,3.42v5l-2,2v1h13.73l2,2L21,19.72l-1,-1.03zM12,22c1.11,0 2,-0.89 2,-2h-4c0,1.11 0.89,2 2,2zM18,14.68L18,11c0,-3.08 -1.64,-5.64 -4.5,-6.32L13.5,4c0,-0.83 -0.67,-1.5 -1.5,-1.5s-1.5,0.67 -1.5,1.5v0.68c-0.15,0.03 -0.29,0.08 -0.42,0.12 -0.1,0.03 -0.2,0.07 -0.3,0.11h-0.01c-0.01,0 -0.01,0 -0.02,0.01 -0.23,0.09 -0.46,0.2 -0.68,0.31 0,0 -0.01,0 -0.01,0.01L18,14.68z"), fill = SolidColor(Color.Black))
        }.build()
        return _notifications_off!!
    }

val Icons.Filled.Person: ImageVector
    get() {
        _person?.let { return it }
        _person = ImageVector.Builder(
            name = "Filled.Person",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M12,12c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4 -4,1.79 -4,4 1.79,4 4,4zM12,14c-2.67,0 -8,1.34 -8,4v2h16v-2c0,-2.66 -5.33,-4 -8,-4z"), fill = SolidColor(Color.Black))
        }.build()
        return _person!!
    }

val Icons.Filled.Public: ImageVector
    get() {
        _public?.let { return it }
        _public = ImageVector.Builder(
            name = "Filled.Public",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM11,19.93c-3.95,-0.49 -7,-3.85 -7,-7.93 0,-0.62 0.08,-1.21 0.21,-1.79L9,15v1c0,1.1 0.9,2 2,2v1.93zM17.9,17.39c-0.26,-0.81 -1,-1.39 -1.9,-1.39h-1v-3c0,-0.55 -0.45,-1 -1,-1L8,12v-2h2c0.55,0 1,-0.45 1,-1L11,7h2c1.1,0 2,-0.9 2,-2v-0.41c2.93,1.19 5,4.06 5,7.41 0,2.08 -0.8,3.97 -2.1,5.39z"), fill = SolidColor(Color.Black))
        }.build()
        return _public!!
    }

val Icons.Filled.SportsEsports: ImageVector
    get() {
        _sports_esports?.let { return it }
        _sports_esports = ImageVector.Builder(
            name = "Filled.SportsEsports",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M21.58,16.09l-1.09,-7.66C20.21,6.46 18.52,5 16.53,5H7.47C5.48,5 3.79,6.46 3.51,8.43l-1.09,7.66C2.2,17.63 3.39,19 4.94,19h0c0.68,0 1.32,-0.27 1.8,-0.75L9,16h6l2.25,2.25c0.48,0.48 1.13,0.75 1.8,0.75h0C20.61,19 21.8,17.63 21.58,16.09zM11,11H9v2H8v-2H6v-1h2V8h1v2h2V11zM15,10c-0.55,0 -1,-0.45 -1,-1c0,-0.55 0.45,-1 1,-1s1,0.45 1,1C16,9.55 15.55,10 15,10zM17,13c-0.55,0 -1,-0.45 -1,-1c0,-0.55 0.45,-1 1,-1s1,0.45 1,1C18,12.55 17.55,13 17,13z"), fill = SolidColor(Color.Black))
        }.build()
        return _sports_esports!!
    }

val Icons.Filled.Star: ImageVector
    get() {
        _star?.let { return it }
        _star = ImageVector.Builder(
            name = "Filled.Star",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(pathData = addPathNodes("M12,17.27L18.18,21l-1.64,-7.03L22,9.24l-7.19,-0.61L12,2 9.19,8.63 2,9.24l5.46,4.73L5.82,21z"), fill = SolidColor(Color.Black))
        }.build()
        return _star!!
    }

private var _assessment: ImageVector? = null
private var _check_circle: ImageVector? = null
private var _delete: ImageVector? = null
private var _help_outline: ImageVector? = null
private var _history: ImageVector? = null
private var _home: ImageVector? = null
private var _lock: ImageVector? = null
private var _lock_open: ImageVector? = null
private var _opacity: ImageVector? = null
private var _open_in_new: ImageVector? = null
private var _picture_in_picture: ImageVector? = null
private var _search: ImageVector? = null
private var _swap_horiz: ImageVector? = null
private var _touch_app: ImageVector? = null
private var _error_outline: ImageVector? = null
private var _warning_amber: ImageVector? = null
private var _games: ImageVector? = null
private var _play_arrow: ImageVector? = null
private var _play_circle: ImageVector? = null
private var _speed: ImageVector? = null
private var _stop_circle: ImageVector? = null
private var _chat: ImageVector? = null
private var _add: ImageVector? = null
private var _bolt: ImageVector? = null
private var _access_time: ImageVector? = null
private var _battery_saver: ImageVector? = null
private var _memory: ImageVector? = null
private var _phone_android: ImageVector? = null
private var _music_note: ImageVector? = null
private var _timer: ImageVector? = null
private var _tune: ImageVector? = null
private var _cleaning_services: ImageVector? = null
private var _arrow_back: ImageVector? = null
private var _close: ImageVector? = null
private var _unfold_less: ImageVector? = null
private var _emoji_events: ImageVector? = null
private var _notifications_off: ImageVector? = null
private var _person: ImageVector? = null
private var _public: ImageVector? = null
private var _sports_esports: ImageVector? = null
private var _star: ImageVector? = null
