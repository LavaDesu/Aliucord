package com.discord.api.botuikit

import java.io.Serializable

abstract class ContentComponent : LayoutComponent(), Serializable {
    override fun a(): List<Component> = listOf()
}
