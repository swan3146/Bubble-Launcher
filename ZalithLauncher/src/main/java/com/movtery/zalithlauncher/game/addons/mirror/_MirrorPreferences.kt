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

package com.movtery.zalithlauncher.game.addons.mirror

import com.movtery.zalithlauncher.setting.enums.MirrorSourceType

/** 设置折算后的镜像使用策略 */
enum class MirrorPriority {
    /** 只使用官方源 */
    OFFICIAL,
    /** 镜像在前、官方在后 */
    MIRROR_FIRST
}

/**
 * 把三档设置折算成镜像使用策略。
 * AUTO 档默认镜像优先：国内用户手机时区偶发不是 Asia/Shanghai（被改过或双卡漫游），
 * 旧逻辑此时完全不走镜像、直连 Mojang/Modrinth 官方源，是“下载非常慢”的根因。
 * 镜像不可达时下载引擎会按候选顺序回退到官方源，故始终镜像优先对海外用户也只是多一次快速失败。
 */
fun resolveMirrorPriority(source: MirrorSourceType, mainland: Boolean): MirrorPriority =
    when (source) {
        MirrorSourceType.OFFICIAL -> MirrorPriority.OFFICIAL
        MirrorSourceType.MIRROR -> MirrorPriority.MIRROR_FIRST
        MirrorSourceType.AUTO -> MirrorPriority.MIRROR_FIRST
    }

/** 按生效策略产出候选列表；官方档不注入镜像，镜像链接不存在时仅保留官方源 */
fun orderCandidates(official: String, mirror: String?, priority: MirrorPriority): List<String> =
    when (priority) {
        MirrorPriority.OFFICIAL -> listOfNotNull(official)
        MirrorPriority.MIRROR_FIRST -> listOfNotNull(mirror, official)
    }
