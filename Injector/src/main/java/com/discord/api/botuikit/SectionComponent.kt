package com.discord.api.botuikit

data class SectionComponent(
    private val type: ComponentType,
    val id: Int?,
    val components: List<Component>,
    val accessory: Component,
): LayoutComponent() {
    override fun getType() = type
    override fun a() = components
}
