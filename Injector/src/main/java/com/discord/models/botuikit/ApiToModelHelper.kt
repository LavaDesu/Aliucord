package com.discord.models.botuikit

import com.discord.api.botuikit.*
import com.discord.widgets.botuikit.ComponentChatListState.ComponentStoreState

@Suppress("UNUSED")
object ApiToModelHelper {
    // Used by com.discord.widgets.botuikit.ComponentStateMapper.createActionMessage in smali patch
    // createActionMessage performs a lot of complicated logic, so a smali patch is easiest
    @JvmStatic
    fun convert(
        component: ActionComponent,
        index: Int,
        defaultState: ActionInteractionComponentState,
        componentStoreState: ComponentStoreState
    ): MessageComponent? {
        return when (component) {
            is ButtonComponent -> ButtonMessageComponentKt.mergeToMessageComponent(component, index, defaultState, componentStoreState)
            is SelectComponent -> SelectMessageComponentKt.mergeToMessageComponent(component, index, defaultState, componentStoreState)
            is EntitySelectComponent -> EntitySelectMessageComponent.mergeToMessageComponent(component, index, defaultState, componentStoreState)
            else -> null
        }
    }
}
