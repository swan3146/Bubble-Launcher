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

package com.movtery.zalithlauncher.ui.screens.content.elements

import com.movtery.zalithlauncher.ui.theme.cardColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.MaterialStyle
import com.movtery.zalithlauncher.ui.screens.content.descRes
import com.movtery.zalithlauncher.ui.screens.content.nameRes

/**
 * PCL Pro Android: 首次启动的材质样式引导弹窗
 * @param onDismiss 关闭回调（选择应用或稍后再说都会回调，由调用方记录引导状态）
 */
@Composable
fun MaterialIntroDialog(onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(AllSettings.launcherMaterialStyle.state) }

    AlertDialog(
        containerColor = cardColor(),
        tonalElevation = 0.dp,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(text = stringResource(R.string.bl_material_intro_title))
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.bl_material_intro_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MaterialStyle.entries.forEach { style ->
                        StyleOption(
                            style = style,
                            selected = style == selected,
                            onClick = { selected = style }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    AllSettings.launcherMaterialStyle.save(selected)
                    onDismiss()
                }
            ) {
                Text(text = stringResource(R.string.bl_material_intro_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.bl_material_intro_later))
            }
        }
    )
}

/**
 * 弹窗内的材质选项行
 */
@Composable
private fun StyleOption(
    style: MaterialStyle,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val border = if (selected) {
        BorderStroke(1.4.dp, scheme.primary.copy(alpha = 0.75f))
    } else {
        BorderStroke(0.8.dp, scheme.outlineVariant.copy(alpha = 0.45f))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) scheme.primary.copy(alpha = 0.10f) else Color.Transparent,
        border = border
    ) {
        Row(
            modifier = Modifier.padding(all = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    when (style) {
                        MaterialStyle.LiquidGlass -> {
                            drawRect(
                                brush = Brush.linearGradient(
                                    listOf(
                                        scheme.primary.copy(alpha = 0.60f),
                                        scheme.tertiary.copy(alpha = 0.35f)
                                    )
                                )
                            )
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.45f),
                                cornerRadius = CornerRadius(10.dp.toPx()),
                                style = Stroke(width = 1.2.dp.toPx())
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.45f),
                                start = Offset(size.width * 0.2f, 0f),
                                end = Offset(0f, size.height),
                                strokeWidth = 3.dp.toPx()
                            )
                        }

                        MaterialStyle.Solid -> {
                            drawRect(color = scheme.surfaceVariant)
                        }
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(style.nameRes()),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(style.descRes()),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }

            if (selected) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = scheme.primary
                )
            }
        }
    }
}
