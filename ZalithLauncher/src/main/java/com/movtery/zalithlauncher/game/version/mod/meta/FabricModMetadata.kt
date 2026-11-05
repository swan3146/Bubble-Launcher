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

package com.movtery.zalithlauncher.game.version.mod.meta

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

data class FabricModMetadata(
    @SerializedName("id")
    val id: String,
    //B-14：Fabric 规范中 name 为可选项（version 亦可能缺失），
    //GSON 反序列化会绕过 Kotlin 空检查直接写入 null，因此必须声明为可空
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("version")
    val version: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("icon")
    val icon: String? = null,
    @SerializedName("authors")
    val authors: List<FabricModAuthor>? = null,
    @SerializedName("contact")
    val contact: Map<String, String>? = null
) {
    @JsonAdapter(FabricModAuthorSerializer::class)
    data class FabricModAuthor(
        @SerializedName("name")
        val name: String? = null
    )

    class FabricModAuthorSerializer : JsonSerializer<FabricModAuthor>,
        JsonDeserializer<FabricModAuthor> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): FabricModAuthor? {
            return if (json.isJsonPrimitive) {
                FabricModAuthor(json.asString)
            } else {
                FabricModAuthor(json.asJsonObject.getAsJsonPrimitive("name")?.asString)
            }
        }

        override fun serialize(
            src: FabricModAuthor?,
            typeOfSrc: Type?,
            context: JsonSerializationContext?
        ): JsonElement? {
            return src?.name?.let { JsonPrimitive(it) } ?: JsonNull.INSTANCE
        }
    }
}