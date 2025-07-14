package com.discord.api.botuikit

abstract class EntitySelectComponent : ActionComponent() {
    abstract val id: Int
    abstract val customId: String
    abstract val placeholder: String
    abstract val defaultValues: List<EntityDefaultValue>?
    abstract val minValues: Int
    abstract val maxValues: Int
    abstract val disabled: Boolean
}
