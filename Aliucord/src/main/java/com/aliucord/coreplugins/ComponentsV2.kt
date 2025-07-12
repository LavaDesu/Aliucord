package com.aliucord.coreplugins

import android.content.Context
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.aliucord.Utils
import com.aliucord.coreplugins.componentsv2.BotUiComponentV2Entry
import com.aliucord.coreplugins.componentsv2.ComponentV2Type
import com.aliucord.coreplugins.componentsv2.models.*
import com.aliucord.coreplugins.componentsv2.views.*
import com.aliucord.entities.CorePlugin
import com.aliucord.patcher.*
import com.aliucord.updater.ManagerBuild
import com.discord.api.botuikit.*
import com.discord.api.channel.Channel
import com.discord.api.role.GuildRole
import com.discord.models.botuikit.ActionRowMessageComponent
import com.discord.models.botuikit.MessageComponent
import com.discord.models.member.GuildMember
import com.discord.models.message.Message
import com.discord.stores.StoreMessageReplies.MessageState
import com.discord.stores.StoreMessageState
import com.discord.stores.StoreThreadMessages
import com.discord.utilities.embed.InviteEmbedModel
import com.discord.utilities.view.extensions.ViewExtensions
import com.discord.widgets.botuikit.*
import com.discord.widgets.botuikit.ComponentChatListState.ComponentStoreState
import com.discord.widgets.botuikit.views.*
import com.discord.widgets.botuikit.views.select.SelectComponentView
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapter
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemBotComponentRow
import com.discord.widgets.chat.list.entries.BotUiComponentEntry
import com.discord.widgets.chat.list.entries.ChatListEntry
import com.discord.widgets.chat.list.model.WidgetChatListModelMessages
import com.lytefast.flexinput.R

internal class ComponentsV2 : CorePlugin(Manifest("ComponentsV2")) {
    override val isHidden: Boolean = true
    override val isRequired: Boolean = true

    override fun start(context: Context) {
        if (!ManagerBuild.hasInjector("2.3.0") || !ManagerBuild.hasPatches("1.3.0")) {
            logger.warn("Base app outdated, cannot enable ComponentsV2")
            return
        }

        patcher.instead<BotComponentExperiments>("isEnabled", ComponentType::class.java) { true }

        patcher.instead<ComponentStateMapper>(
            "toMessageLayoutComponent",
            LayoutComponent::class.java,
            Int::class.javaPrimitiveType!!,
            List::class.java,
            ComponentExperiments::class.java
        ) { (_, layout: LayoutComponent, index: Int, components: List<MessageComponent>) ->
            when (layout) {
                is ActionRowComponent ->
                    ActionRowMessageComponent(layout.type, index, components)
                is SectionComponent ->
                    SectionMessageComponent.mergeToMessageComponent(layout, index, components)
                is TextDisplayComponent ->
                    TextDisplayMessageComponent.mergeToMessageComponent(layout, index)
                is ThumbnailComponent ->
                    ThumbnailMessageComponent.mergeToMessageComponent(layout, index)
                is MediaGalleryComponent ->
                    ActionRowMessageComponent(layout.type, index, components)
                is FileComponent ->
                    ActionRowMessageComponent(layout.type, index, components)
                is SeparatorComponent ->
                    SeparatorMessageComponent.mergeToMessageComponent(layout, index)
                is ContainerComponent ->
                    ContainerMessageComponent.mergeToMessageComponent(layout, index, components)
                else ->
                    throw IllegalArgumentException("Unknown layout component ${layout::class.java.name} (${layout.type.type}:${layout.type.name})")
            }
        }

        patcher.instead<ComponentProvider>("configureView", ComponentActionListener::class.java, MessageComponent::class.java, ComponentView::class.java)
        { (_, listener: ComponentActionListener, component: MessageComponent, view: ComponentView<MessageComponent>?) ->
            view?.configure(component, this, listener)
        }

        // love
        @Suppress("UNUSED_DESTRUCTURED_PARAMETER_ENTRY", "LocalVariableName")
        patcher.patch(WidgetChatListModelMessages.Companion::class.java.declaredMethods.find { it.name == "getMessageItems" }!!)
        {(
            param,
            channel: Channel,
            guildMembers: Map<Long, GuildMember>,
            guildRoles: Map<Long, GuildRole>,
            _blockedRelationships: Map<Long, Int>?,
            _referencedChannel: Channel?,
            _threadStoreState: StoreThreadMessages.ThreadState?,
            _message: Message,
            state: StoreMessageState.State?,
            _repliedMessages: Map<Long, MessageState>?,
            _isBlockedExpanded: Boolean,
            _isMinimal: Boolean,
            _permissions: Long?,
            _allowAnimatedEmojis: Boolean,
            _autoPlayGifs: Boolean,
            _isRenderEmbedEnabled: Boolean,
            meId: Long,
            _isRenderComponentEnabled: Boolean,
            _componentStoreState: Map<Long, ComponentStoreState>,
            _inviteEmbedModel: InviteEmbedModel?,
            _isThreadStarterMessage: Boolean,
            _canGuildSeePurchaseFeedbackLoopSystemMessages: Boolean
        ) ->
            @Suppress("UNCHECKED_CAST")
            val result = (param.result as MutableList<ChatListEntry>)
            result.forEachIndexed { index, entry ->
                if (entry is BotUiComponentEntry && ((entry.message.flags shr 15) == 1L)) {
                    val fields = BotUiComponentV2Entry.V2Fields(state, meId, channel, guildMembers, guildRoles)
                    result[index] = BotUiComponentV2Entry.fromV1(entry, fields)
                }
            }
        }

        patcher.instead<ComponentInflater>("inflateComponent", ComponentType::class.java, ViewGroup::class.java)
        { (_, type: ComponentType, viewGroup: ViewGroup) ->
            when (type) {
                ComponentType.ACTION_ROW ->
                    ActionRowComponentView.Companion!!.inflateComponent(this.context, viewGroup)
                ComponentType.BUTTON ->
                    ButtonComponentView.Companion!!.inflateComponent(this.context, viewGroup)
                ComponentType.SELECT ->
                    SelectComponentView.Companion!!.inflateComponent(this.context, viewGroup)
                ComponentV2Type.USER_SELECT,
                ComponentV2Type.ROLE_SELECT,
                ComponentV2Type.MENTIONABLE_SELECT,
                ComponentV2Type.CHANNEL_SELECT ->
                    null
                ComponentV2Type.SECTION ->
                    SectionComponentView(this.context)
                ComponentV2Type.TEXT_DISPLAY ->
                    TextDisplayComponentView(this.context)
                ComponentV2Type.THUMBNAIL ->
                    ThumbnailComponentView(this.context)
                ComponentV2Type.MEDIA_GALLERY ->
                    null
                ComponentV2Type.FILE ->
                    null
                ComponentV2Type.SEPARATOR ->
                    SeparatorComponentView(this.context)
                ComponentV2Type.CONTAINER ->
                    ContainerComponentView(this.context)
                else -> null
            }
        }

        patcher.after<WidgetChatListAdapterItemBotComponentRow>(WidgetChatListAdapter::class.java)
        {
            itemView.layoutParams = itemView.layoutParams.apply {
                width = WRAP_CONTENT
            }
            val rootLayout = itemView.findViewById<LinearLayout>(Utils.getResId("chat_list_adapter_item_component_root", "id"))
            rootLayout.layoutParams = (rootLayout.layoutParams as ConstraintLayout.LayoutParams).apply {
                marginEnd = adapter.context.resources.getDimension(R.d.chat_cell_horizontal_spacing_padding).toInt()
            }

            ViewExtensions.setOnLongClickListenerConsumeClick(itemView) {
                adapter.eventHandler.onMessageLongClicked(entry.message, "", false)
            }
            itemView.setOnClickListener {
                adapter.eventHandler.onMessageClicked(entry.message, false)
            }
        }
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}
