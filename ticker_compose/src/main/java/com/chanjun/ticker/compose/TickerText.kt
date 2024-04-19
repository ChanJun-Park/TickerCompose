package com.chanjun.ticker.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.collectLatest

private val DEFAULT_FONT_SIZE = 16.sp
private val DEFAULT_LINE_HEIGHT = 16.sp

@Composable
fun TickerText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = DEFAULT_FONT_SIZE,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = DEFAULT_LINE_HEIGHT,
    style: TextStyle = LocalTextStyle.current,
    vararg characterLists: String = arrayOf(
        TickerUtils.provideNumberList(),
        TickerUtils.provideAlphabeticalList()
    )
) {
    check(fontSize != TextUnit.Unspecified && lineHeight != TextUnit.Unspecified) {
        "fontSize or lineHeight must not be set to TextUnit.Unspecified"
    }

    val textMeasurer = rememberTextMeasurer()
    val textColor = color.takeOrElse {
        style.color.takeOrElse {
            LocalContentColor.current
        }
    }

    val mergedStyle = style.merge(
        TextStyle(
            color = textColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign,
            lineHeight = lineHeight,
            fontFamily = fontFamily,
            textDecoration = textDecoration,
            fontStyle = fontStyle,
            letterSpacing = letterSpacing,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None
            ),
            platformStyle = PlatformTextStyle(
                includeFontPadding = false
            )
        )
    )

    val tickerColumnManager = rememberTickerColumnManager(
        textMeasurer = textMeasurer,
        textStyle = mergedStyle,
        characterLists = characterLists.toList().toImmutableList()
    )

    val animationProgress = remember {
        Animatable(0f)
    }

    var width by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val widthInDp = with(density) {
        width.toDp()
    }

    val heightInDp = with(density) {
        lineHeight.takeOrElse {
            fontSize.takeOrElse {
                DEFAULT_LINE_HEIGHT
            }
        }.toDp()
    }

    LaunchedEffect(text) {
        tickerColumnManager.setText(text.toCharArray())

        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f)
    }

    val isInspectionMode = LocalInspectionMode.current
    if (isInspectionMode) {
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign,
            lineHeight = lineHeight,
            style = style,
            modifier = modifier
        )
    } else {
        Box(modifier) {
            Canvas(
                Modifier
                    .width(widthInDp)
                    .height(heightInDp)
                    .align(
                        when (textAlign) {
                            TextAlign.Left -> AbsoluteAlignment.TopLeft
                            TextAlign.Right -> AbsoluteAlignment.TopRight
                            TextAlign.End -> Alignment.TopEnd
                            TextAlign.Center -> Alignment.TopCenter
                            // TextAlign.Start, TextAlign.Justify, null
                            else -> Alignment.TopStart
                        }
                    )
            ) {
                clipRect {
                    tickerColumnManager.draw(this, animationProgress.value)
                }
            }
        }
    }

    LaunchedEffect(text, tickerColumnManager) {
        snapshotFlow {
            animationProgress.value
        }.collect {
            width = tickerColumnManager.getCurrentWidth()
        }
    }

    LaunchedEffect(text, tickerColumnManager) {
        snapshotFlow {
            animationProgress.isRunning
        }.collectLatest { isRunning ->
            if (isRunning.not()) {
                tickerColumnManager.onAnimationEnd()
            }
        }
    }
}

@Preview
@Composable
fun TickerTextPreview() {
    Column {
        var inputText by remember {
            mutableStateOf("")
        }

        var textToShow by remember {
            mutableStateOf("test")
        }

        TickerText(
            text = textToShow,
            color = Color.Black,
            textAlign = TextAlign.Left,
            fontSize = 30.sp,
            lineHeight = 34.sp,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(color = Color.White)
        )

        TextField(
            value = inputText,
            onValueChange = { inputText = it }
        )

        Button(onClick = { textToShow = inputText }) {
            Text(text = "submit")
        }
    }
}