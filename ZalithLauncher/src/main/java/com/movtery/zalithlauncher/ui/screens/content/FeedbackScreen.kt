/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.ui.screens.content

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.path.URL_PROJECT
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.AnimatedLazyColumn
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.theme.onCardColor
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import com.movtery.zalithlauncher.viewmodel.sendToast

/** 反馈类型 */
private val feedbackTypes = listOf(
    R.string.bl_feedback_type_suggestion,
    R.string.bl_feedback_type_issue,
    R.string.bl_feedback_type_other
)

/**
 * PCL Pro Android: 意见反馈页面
 * 填写完成后一键复制反馈内容并跳转至议题页面
 */
@Composable
fun FeedbackScreen(
    key: NormalNavKey.Feedback,
    backScreenViewModel: ScreenBackStackViewModel,
    eventViewModel: EventViewModel,
    openLink: (url: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val mainScreenKey = backScreenViewModel.mainScreen.currentKey

    var selectedType by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }

    BaseScreen(
        Triple(key, mainScreenKey, false)
    ) { isVisible ->
        AnimatedLazyColumn(
            modifier = Modifier.fillMaxSize(),
            isVisible = isVisible,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(all = 12.dp)
        ) { scope ->
            animatedItem(scope) { yOffset ->
                FeedbackBanner(
                    modifier = Modifier.offset { IntOffset(x = 0, y = yOffset.roundToPx()) }
                )
            }

            animatedItem(scope) { yOffset ->
                BackgroundCard(
                    modifier = Modifier
                        .offset { IntOffset(x = 0, y = yOffset.roundToPx()) }
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(all = 16.dp)) {
                        Text(
                            text = stringResource(R.string.bl_feedback_type_title),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            feedbackTypes.forEachIndexed { index, res ->
                                TypeChip(
                                    text = stringResource(res),
                                    selected = index == selectedType,
                                    onClick = { selectedType = index }
                                )
                            }
                        }
                    }
                }
            }

            animatedItem(scope) { yOffset ->
                BackgroundCard(
                    modifier = Modifier
                        .offset { IntOffset(x = 0, y = yOffset.roundToPx()) }
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(all = 16.dp)) {
                        Text(
                            text = stringResource(R.string.bl_feedback_content_title),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = message,
                            onValueChange = { if (it.length <= 2000) message = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            minLines = 5,
                            maxLines = 12,
                            placeholder = {
                                Text(text = stringResource(R.string.bl_feedback_content_hint))
                            },
                            supportingText = {
                                Text(
                                    text = stringResource(
                                        R.string.bl_feedback_counter,
                                        message.length
                                    )
                                )
                            }
                        )
                    }
                }
            }

            animatedItem(scope) { yOffset ->
                BackgroundCard(
                    modifier = Modifier
                        .offset { IntOffset(x = 0, y = yOffset.roundToPx()) }
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(all = 16.dp)) {
                        Text(
                            text = stringResource(R.string.bl_feedback_contact_title),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = contact,
                            onValueChange = { if (it.length <= 120) contact = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            placeholder = {
                                Text(text = stringResource(R.string.bl_feedback_contact_hint))
                            }
                        )
                    }
                }
            }

            animatedItem(scope) { yOffset ->
                Column(
                    modifier = Modifier
                        .offset { IntOffset(x = 0, y = yOffset.roundToPx()) }
                        .fillMaxWidth()
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val content = message.trim()
                            if (content.isEmpty()) {
                                eventViewModel.sendToast(androidText(R.string.bl_feedback_empty))
                                return@Button
                            }
                            val typeLabel = context.getString(feedbackTypes[selectedType])
                            val text = buildString {
                                append("[" + context.getString(R.string.bl_feedback_type_title) + "] ")
                                append(typeLabel)
                                append('\n')
                                append("[" + context.getString(R.string.bl_feedback_content_title) + "] ")
                                append(content)
                                append('\n')
                                append("[Launcher] v")
                                append(BuildConfig.VERSION_NAME)
                                append(" / Android ")
                                append(Build.VERSION.RELEASE)
                                append(" / ")
                                append(Build.MODEL)
                                if (contact.isNotBlank()) {
                                    append('\n')
                                    append("[" + context.getString(R.string.bl_feedback_contact_title) + "] ")
                                    append(contact.trim())
                                }
                            }

                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(
                                ClipData.newPlainText("PCL Pro Feedback", text)
                            )

                            eventViewModel.sendToast(androidText(R.string.bl_feedback_copied))
                            openLink("$URL_PROJECT/issues")
                            onBack()
                        }
                    ) {
                        Text(text = stringResource(R.string.bl_feedback_submit))
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.bl_feedback_submit_hint),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelSmall,
                        color = onCardColor().copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}

/**
 * 顶部横幅
 */
@Composable
private fun FeedbackBanner(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme

    BackgroundCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            scheme.secondary.copy(alpha = 0.30f),
                            scheme.primary.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f),
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            modifier = Modifier.size(26.dp),
                            painter = painterResource(R.drawable.ic_chat_info),
                            contentDescription = null
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.bl_feedback_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.bl_feedback_summary),
                        style = MaterialTheme.typography.labelSmall,
                        color = onCardColor().copy(alpha = 0.68f)
                    )
                }
            }
        }
    }
}

/**
 * 反馈类型选择标签
 */
@Composable
private fun TypeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
        } else {
            Color.Transparent
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            onCardColor().copy(alpha = 0.75f)
        },
        border = BorderStroke(
            width = if (selected) 1.2.dp else 0.8.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            } else {
                Color.White.copy(alpha = 0.10f)
            }
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}
