package com.discord.models.botuikit

import com.discord.api.botuikit.*
import com.discord.widgets.botuikit.ComponentChatListState

// This needs to be accessed by ApiToModelHelper, so it also needs to be part of injector
data class SelectV2MessageComponent(
    private val type: ComponentType,
    private val index: Int,
    private val stateInteraction: ActionInteractionComponentState,

    val id: Int,
    val customId: String,
    val placeholder: String,
    val minValues: Int,
    val maxValues: Int,
    val defaultValues: List<SelectV2DefaultValue>,
    val emojiAnimationsEnabled: Boolean,
) : ActionMessageComponent() {
    override fun getType() = type
    override fun getIndex() = index
    override fun getStateInteraction() = stateInteraction

    companion object {
        fun mergeToMessageComponent(
            selectComponent: SelectV2Component,
            index: Int,
            stateInteraction: ActionInteractionComponentState,
            componentStoreState: ComponentChatListState.ComponentStoreState
        ): SelectV2MessageComponent {
            return selectComponent.run {
                SelectV2MessageComponent(
                    type,
                    index,
                    stateInteraction,
                    id,
                    customId,
                    placeholder,
                    minValues,
                    maxValues,
                    defaultValues ?: listOf(),
                    componentStoreState.animateEmojis
                )
            }
        }
    }
}
