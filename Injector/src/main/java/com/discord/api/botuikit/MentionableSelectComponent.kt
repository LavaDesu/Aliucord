package com.discord.api.botuikit

data class MentionableSelectComponent(
    private val type: ComponentType,
    override val id: Int?,
    @b.i.d.p.b("custom_id") override val customId: String,
    override val placeholder: String,
    override val defaultValues: List<EntityDefaultValue>,
    override val minValues: Int,
    override val maxValues: Int,
    override val disabled: Boolean,
) : EntitySelectComponent() {
    override fun getType() = type
}
