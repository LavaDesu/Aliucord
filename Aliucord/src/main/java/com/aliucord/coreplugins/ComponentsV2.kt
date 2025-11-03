package com.aliucord.coreplugins

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.aliucord.Utils
import com.aliucord.coreplugins.componentsv2.models.*
import com.aliucord.coreplugins.componentsv2.views.*
import com.aliucord.entities.CorePlugin
import com.aliucord.patcher.*
import com.aliucord.updater.ManagerBuild
import com.discord.api.botuikit.*
import com.discord.api.channel.Channel
import com.discord.api.role.GuildRole
import com.discord.models.botuikit.*
import com.discord.models.member.GuildMember
import com.discord.models.message.Message
import com.discord.stores.StoreApplicationInteractions.InteractionSendState
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

val Message.isComponentV2 get() = ((flags ?: 0) shr 15) and 1 == 1L

internal class ComponentsV2 : CorePlugin(Manifest("ComponentsV2")) {
    override val isHidden: Boolean = true
    override val isRequired: Boolean = true

    override fun start(context: Context) {
        if (!ManagerBuild.hasInjector("2.4.0") || !ManagerBuild.hasPatches("1.4.0")) {
            logger.warn("Base app outdated, cannot enable ComponentsV2")
            return
        }

        patchMessageItems()

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
                    MediaGalleryMessageComponent.mergeToMessageComponent(layout, index)
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

        patcher.instead<ComponentInflater>("inflateComponent", ComponentType::class.java, ViewGroup::class.java)
        { (_, type: ComponentType, viewGroup: ViewGroup) ->
            when (type) {
                ComponentType.ACTION_ROW ->
                    ActionRowComponentView.Companion!!.inflateComponent(this.context, viewGroup)
                ComponentType.BUTTON ->
                    ButtonComponentView.Companion!!.inflateComponent(this.context, viewGroup)
                ComponentType.SELECT ->
                    SelectComponentView.Companion!!.inflateComponent(this.context, viewGroup)
                ComponentType.USER_SELECT,
                ComponentType.ROLE_SELECT,
                ComponentType.MENTIONABLE_SELECT,
                ComponentType.CHANNEL_SELECT ->
                    SelectV2ComponentView(this.context, type)
                ComponentType.SECTION ->
                    SectionComponentView(this.context)
                ComponentType.TEXT_DISPLAY ->
                    TextDisplayComponentView(this.context)
                ComponentType.THUMBNAIL ->
                    ThumbnailComponentView(this.context)
                ComponentType.MEDIA_GALLERY ->
                    MediaGalleryComponentView(this.context)
                ComponentType.FILE ->
                    null
                ComponentType.SEPARATOR ->
                    SeparatorComponentView(this.context)
                ComponentType.CONTAINER ->
                    ContainerComponentView(this.context)
                else -> null
            }
        }

        patcher.after<WidgetChatListAdapterItemBotComponentRow>(WidgetChatListAdapter::class.java)
        {
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

        patcher.instead<ComponentStateMapper>(
            "createActionMessageComponent",
            ActionComponent::class.java,
            Int::class.javaPrimitiveType!!,
            ComponentStoreState::class.java,
            ComponentExperiments::class.java,
        ) { (
            _,
            actionComponent: ActionComponent,
            index: Int,
            componentStoreState: ComponentStoreState,
        ) ->
            val interactionState: Map<Int, InteractionSendState>? = componentStoreState.interactionState;
            val num = interactionState?.entries?.find { it.value is InteractionSendState.Loading }?.key

            val state = interactionState?.get(index)
            val comState: ActionInteractionComponentState = when {
                state is InteractionSendState.Failed -> ActionInteractionComponentState.Failed(state.errorMessage)
                num == null -> ActionInteractionComponentState.Enabled.INSTANCE
                num == index -> ActionInteractionComponentState.Loading.INSTANCE
                else -> ActionInteractionComponentState.Disabled.INSTANCE
            }

            when (actionComponent) {
                is ButtonComponent ->
                    ButtonMessageComponentKt.mergeToMessageComponent(actionComponent, index, comState, componentStoreState)
                is SelectComponent ->
                    SelectMessageComponentKt.mergeToMessageComponent(actionComponent, index, comState, componentStoreState)
                is SelectV2Component ->
                    SelectV2MessageComponent.mergeToMessageComponent(actionComponent, index, comState, componentStoreState)
                else -> null
            }
        }

        patcher.after<Message>("shouldShowReplyPreviewAsAttachment") { param ->
            if (this.isComponentV2) param.result = true
        }
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }

    fun patchMessageItems() {
        @Suppress("UNUSED_DESTRUCTURED_PARAMETER_ENTRY", "LocalVariableName", "UnusedVariable")
        patcher.patch(
            WidgetChatListModelMessages.Companion::class.java.declaredMethods.find { it.name == "getMessageItems" }!!
        ) {(
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
                if (entry is BotUiComponentEntry && entry.message.isComponentV2) {
                    entry.copy(
                        state = state,
                        meId = meId,
                        channel = channel,
                        guildMembers = guildMembers,
                        guildRoles = guildRoles,
                    )
                }
            }
        }
    }
}
