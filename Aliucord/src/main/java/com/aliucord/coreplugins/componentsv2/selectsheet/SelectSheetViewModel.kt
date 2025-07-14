@file:Suppress("MISSING_DEPENDENCY_CLASS", "MISSING_DEPENDENCY_SUPERCLASS")

package com.aliucord.coreplugins.componentsv2.selectsheet

import androidx.lifecycle.ViewModel
import com.aliucord.coreplugins.componentsv2.BotUiComponentV2Entry
import com.aliucord.coreplugins.componentsv2.ComponentV2Type
import com.aliucord.wrappers.ChannelWrapper.Companion.id
import com.discord.models.botuikit.SelectV2MessageComponent
import com.discord.stores.StoreStream

const val ENTRY_LIMIT = 15

internal class SelectSheetViewModel() : ViewModel() {
    data class ViewState(
        val placeholder: String,
        val items: List<SelectSheetItem>,
        val showSelectButton: Boolean,
        val isMultiSelect: Boolean,
        val minSelections: Int,
        val maxSelections: Int,
        val isValidSelection: Boolean,
    )

    var onUpdate: ((ViewState) -> Unit)? = null

    var state: ViewState? = null
        set(value) {
            field = value
            value?.let { onUpdate?.invoke(it) }
        }

    fun configure(entry: BotUiComponentV2Entry, component: SelectV2MessageComponent) {
        var entryCount = 0
        val items = mutableListOf<SelectSheetItem>()
        val users = StoreStream.getUsers().users
        if (component.type in listOf(ComponentV2Type.USER_SELECT, ComponentV2Type.MENTIONABLE_SELECT)) {
            for (member in entry.guildMembers.values) {
                entryCount += 1
                if (entryCount > ENTRY_LIMIT)
                    break
                val user = users[member.userId]!!
                val isDefault = component.defaultValues.any { it.id == member.userId }
                items.add(SelectSheetItem.UserSelectItem(isDefault, user, member))
            }
        }
        if (component.type in listOf(ComponentV2Type.ROLE_SELECT, ComponentV2Type.MENTIONABLE_SELECT)) {
            for (role in entry.guildRoles.values) {
                entryCount += 1
                if (entryCount > ENTRY_LIMIT)
                    break
                val isDefault = component.defaultValues.any { it.id == role.id }
                items.add(SelectSheetItem.RoleSelectItem(isDefault, role))
            }
        }
        if (component.type == ComponentV2Type.CHANNEL_SELECT) {
            val channels = StoreStream.getChannels().getChannelsForGuild(entry.guildId)!!
            for (channel in channels.values) {
                entryCount += 1
                if (entryCount > ENTRY_LIMIT)
                    break
                val isDefault = component.defaultValues.any { it.id == channel.id }
                items.add(SelectSheetItem.ChannelSelectItem(isDefault, channel))
            }
        }

        val min = component.minValues
        val max = component.maxValues
        val isValidSelection = true
        state = ViewState(
            component.placeholder,
            items,
            showSelectButton = max > 1,
            isMultiSelect = max > 1,
            minSelections = min,
            maxSelections = max,
            isValidSelection,
        )
    }

    fun toggle(item: SelectSheetItem) {
        val state = state ?: return
        val newItems = state.items.map {
            if (it == item)
                item.copy(checked = !item.checked)
            else
                it
        }
        this.state = state.copy(items = newItems)
    }
}
