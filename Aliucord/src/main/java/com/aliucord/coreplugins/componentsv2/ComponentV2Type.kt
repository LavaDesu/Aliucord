package com.aliucord.coreplugins.componentsv2

import com.discord.api.botuikit.ComponentType

// TODO: Due to a bug, these values added by Injector can't be accessed directly.
// Once gradle plugin v2 rolls around, we can replace ComponentV2Type with ComponentType.
object ComponentV2Type {
    val USER_SELECT = ComponentType.values()[5]
    val ROLE_SELECT = ComponentType.values()[6]
    val MENTIONABLE_SELECT = ComponentType.values()[7]
    val CHANNEL_SELECT = ComponentType.values()[8]
    val SECTION = ComponentType.values()[9]
    val TEXT_DISPLAY = ComponentType.values()[10]
    val THUMBNAIL = ComponentType.values()[11]
    val MEDIA_GALLERY = ComponentType.values()[12]
    val FILE = ComponentType.values()[13]
    val SEPARATOR = ComponentType.values()[14]
    val CONTAINER = ComponentType.values()[15]
    val LABEL = ComponentType.values()[16]
    val FILE_UPLOAD = ComponentType.values()[17]
}
