package com.aliucord.coreplugins.decorations.nameplate

import android.content.Context
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
import com.aliucord.coreplugins.decorations.Decorator
import com.aliucord.utils.DimenUtils.dp
import com.aliucord.utils.ViewUtils.addTo
import com.aliucord.utils.ViewUtils.leftPadding
import com.aliucord.utils.ViewUtils.rightPadding
import com.aliucord.utils.accessField
import com.aliucord.wrappers.users.collectibles
import com.discord.api.user.Collectibles
import com.discord.databinding.WidgetChannelMembersListItemUserBinding
import com.discord.stores.StoreStream
import com.discord.utilities.icon.IconUtils
import com.discord.views.sticker.StickerView
import com.discord.widgets.channels.list.WidgetChannelsListAdapter
import com.discord.widgets.channels.list.items.ChannelListItemPrivate
import com.discord.widgets.channels.memberlist.adapter.ChannelMembersListAdapter
import com.discord.widgets.channels.memberlist.adapter.ChannelMembersListViewHolderMember
import com.facebook.drawee.view.SimpleDraweeView

private val ChannelMembersListViewHolderMember.binding by accessField<WidgetChannelMembersListItemUserBinding>()

private const val rightPad = 0
private const val decoAlpha = 0.6f

internal class NameplateDecorator() : Decorator() {
    private val decoId = View.generateViewId()

    private fun createDecoView(context: Context, animated: Boolean): View {
        val deco = if (animated) {
            // Removes built-in padding
            StickerView(context, null).apply { j.b.setPadding(0, 0, 0, 0) }
        } else {
            SimpleDraweeView(context)
        }

        return deco.apply {
            id = decoId
            alpha = decoAlpha
        }
    }

    private fun findAndConfigure(parent: View, data: Collectibles.Nameplate?) {
        parent.findViewById<View>(decoId)?.run {
            if (data != null) {
                visibility = View.VISIBLE
                if (this is StickerView) {
                    // d(DecorationSticker(data), null)
                } else if (this is SimpleDraweeView) {
                    IconUtils.setIcon(
                        this,
                        "https://cdn.discordapp.com/assets/collectibles/${data.asset}img.png?passthrough=true",
                    )
                }
                background = Palette.from(data.palette).drawable()
            } else {
                visibility = View.INVISIBLE
            }
        }
    }

    override fun onDMsInit(
        holder: WidgetChannelsListAdapter.ItemChannelPrivate,
        adapter: WidgetChannelsListAdapter
    ) {
    }

    override fun onDMsConfigure(
        holder: WidgetChannelsListAdapter.ItemChannelPrivate,
        item: ChannelListItemPrivate
    ) {
    }

    override fun onMembersListInit(
        holder: ChannelMembersListViewHolderMember,
        binding: WidgetChannelMembersListItemUserBinding
    ) {
        val layout = binding.a
        val avatarView = binding.b
        val boostedIndicator = binding.c
        val ownerIndicator = binding.e
        val usernameView = binding.f
        val rpcIconView = binding.h

        // Clear the layout's padding and add our own
        layout.leftPadding = 0
        layout.rightPadding = 0
        avatarView.layoutParams = (avatarView.layoutParams as ConstraintLayout.LayoutParams).apply {
            marginStart = 16.dp
        }

        // Move the boosted and owner indicators so they don't block nameplates
        ownerIndicator.layoutParams = (ownerIndicator.layoutParams as ConstraintLayout.LayoutParams).apply {
            topToTop = usernameView.id
            bottomToBottom = usernameView.id
            startToEnd = usernameView.id
        }
        boostedIndicator.layoutParams = (boostedIndicator.layoutParams as ConstraintLayout.LayoutParams).apply {
            topToTop = usernameView.id
            bottomToBottom = usernameView.id
            startToEnd = ownerIndicator.id
            endToEnd = UNSET
            rightToRight = UNSET
        }

        // Fixup username and rpc icon's constraints (Thanks Discord, these were completely unnecessary)
        usernameView.layoutParams = (usernameView.layoutParams as ConstraintLayout.LayoutParams).apply {
            rightToRight = UNSET
            endToStart = UNSET
        }
        rpcIconView.layoutParams = (rpcIconView.layoutParams as ConstraintLayout.LayoutParams).apply {
            rightToLeft = UNSET
            endToStart = UNSET
        }

        // Add the nameplate view
        createDecoView(layout.context, false).addTo(layout) {
            layoutParams = ConstraintLayout.LayoutParams(0, 0).apply {
                dimensionRatio = "W,16:3"
                rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = 1.dp
                bottomMargin = 1.dp
                rightMargin = rightPad.dp
            }
        }
    }

    override fun onMembersListConfigure(
        holder: ChannelMembersListViewHolderMember,
        item: ChannelMembersListAdapter.Item.Member,
        adapter: ChannelMembersListAdapter
    ) {
        val layout = holder.binding.a

        val user = StoreStream.getUsers().users[item.userId]
        val member = StoreStream.getGuilds().getMember(item.guildId ?: -1, item.userId)
        val data = member?.collectibles?.nameplate ?: user?.collectibles?.nameplate
        findAndConfigure(layout, data)
    }
}
