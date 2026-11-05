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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.AnimatedLazyColumn
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.theme.onCardColor
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel

/** 免责声明分段（标题资源 到 正文资源） */
private val disclaimerSections = listOf(
    R.string.bl_disclaimer_sec1_title to R.string.bl_disclaimer_sec1_body,
    R.string.bl_disclaimer_sec2_title to R.string.bl_disclaimer_sec2_body,
    R.string.bl_disclaimer_sec3_title to R.string.bl_disclaimer_sec3_body,
    R.string.bl_disclaimer_sec4_title to R.string.bl_disclaimer_sec4_body,
    R.string.bl_disclaimer_sec5_title to R.string.bl_disclaimer_sec5_body,
    R.string.bl_disclaimer_sec6_title to R.string.bl_disclaimer_sec6_body,
    R.string.bl_disclaimer_sec7_title to R.string.bl_disclaimer_sec7_body
)

/**
 * PCL Pro Android: 免责声明页面
 */
@Composable
fun DisclaimerScreen(
    key: NormalNavKey.Disclaimer,
    backScreenViewModel: ScreenBackStackViewModel,
    onBack: () -> Unit
) {
    val mainScreenKey = backScreenViewModel.mainScreen.currentKey

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
                DisclaimerBanner(
                    modifier = Modifier.offset { IntOffset(x = 0, y = yOffset.roundToPx()) }
                )
            }

            animatedItems(scope, disclaimerSections) { index, item, yOffset ->
                SectionCard(
                    modifier = Modifier.offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                    index = index + 1,
                    title = stringResource(item.first),
                    body = stringResource(item.second)
                )
            }

            animatedItem(scope) { yOffset ->
                Column(
                    modifier = Modifier
                        .offset { IntOffset(x = 0, y = yOffset.roundToPx()) }
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.bl_disclaimer_footer),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelSmall,
                        color = onCardColor().copy(alpha = 0.55f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onBack
                    ) {
                        Text(text = stringResource(R.string.bl_disclaimer_confirm))
                    }
                }
            }
        }
    }
}

/**
 * 顶部横幅：图标 + 标题 + 说明
 */
@Composable
private fun DisclaimerBanner(modifier: Modifier = Modifier) {
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
                            scheme.primary.copy(alpha = 0.30f),
                            scheme.tertiary.copy(alpha = 0.12f),
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
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            modifier = Modifier.size(28.dp),
                            painter = painterResource(R.drawable.ic_pcl_shield),
                            contentDescription = null
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.bl_disclaimer_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.bl_disclaimer_updated),
                        style = MaterialTheme.typography.labelSmall,
                        color = onCardColor().copy(alpha = 0.65f)
                    )
                }
            }
        }
    }
}

/**
 * 免责声明正文段落卡片
 */
@Composable
private fun SectionCard(
    index: Int,
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    BackgroundCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            //序号小圆标
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = index.toString(),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = onCardColor().copy(alpha = 0.76f)
                )
            }

            Spacer(modifier = Modifier.width(0.dp))
        }
    }
}
