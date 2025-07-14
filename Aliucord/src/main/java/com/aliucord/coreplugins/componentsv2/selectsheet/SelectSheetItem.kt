package com.aliucord.coreplugins.componentsv2.selectsheet

import com.aliucord.wrappers.ChannelWrapper.Companion.id
import com.discord.api.channel.Channel
import com.discord.api.role.GuildRole
import com.discord.models.member.GuildMember
import com.discord.models.user.User
import com.discord.utilities.mg_recycler.MGRecyclerDataPayload

sealed class SelectSheetItem(
    private val type: Int,
    val id: Long,
) : MGRecyclerDataPayload {
    override fun getKey() = id.toString()
    override fun getType() = type

    abstract val checked: Boolean
    abstract fun copy(checked: Boolean = this.checked) : SelectSheetItem

    internal data class UserSelectItem(
        override val checked: Boolean,
        val user: User,
        val member: GuildMember,
    ) : SelectSheetItem(1, user.id) {
        override fun copy(checked: Boolean): SelectSheetItem = copy(checked = checked, user = user)
    }

    internal data class RoleSelectItem(
        override val checked: Boolean,
        val role: GuildRole,
    ) : SelectSheetItem(2, role.id) {
        override fun copy(checked: Boolean): SelectSheetItem = copy(checked = checked, role = role)
    }

    internal data class ChannelSelectItem(
        override val checked: Boolean,
        val channel: Channel,
    ) : SelectSheetItem(3, channel.id) {
        override fun copy(checked: Boolean): SelectSheetItem = copy(checked = checked, channel = channel)
    }
}
