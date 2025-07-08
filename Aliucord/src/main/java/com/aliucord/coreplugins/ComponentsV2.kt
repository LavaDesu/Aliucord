package com.aliucord.coreplugins

import android.content.Context
import android.view.ViewGroup
import com.aliucord.coreplugins.componentsv2.BotUiComponentV2Entry
import com.aliucord.coreplugins.componentsv2.ComponentTypeExtension
import com.aliucord.coreplugins.componentsv2.models.TextDisplayMessageComponent
import com.aliucord.coreplugins.componentsv2.views.TextDisplayComponentView
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
import com.discord.widgets.botuikit.*
import com.discord.widgets.botuikit.ComponentChatListState.ComponentStoreState
import com.discord.widgets.botuikit.views.*
import com.discord.widgets.botuikit.views.select.SelectComponentView
import com.discord.widgets.chat.list.entries.BotUiComponentEntry
import com.discord.widgets.chat.list.entries.ChatListEntry
import com.discord.widgets.chat.list.model.WidgetChatListModelMessages

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
                    ActionRowMessageComponent(layout.type, index, components)
                is TextDisplayComponent ->
                    TextDisplayMessageComponent.mergeToMessageComponent(layout, index)
                is ThumbnailComponent ->
                    ActionRowMessageComponent(layout.type, index, components)
                is MediaGalleryComponent ->
                    ActionRowMessageComponent(layout.type, index, components)
                is FileComponent ->
                    ActionRowMessageComponent(layout.type, index, components)
                is SeparatorComponent ->
                    ActionRowMessageComponent(layout.type, index, components)
                is ContainerComponent ->
                    ActionRowMessageComponent(layout.type, index, components)
                else ->
                    throw IllegalArgumentException("Unknown layout component ${layout::class.java.name} (${layout.type.type}:${layout.type.name})")
            }
        }

        patcher.instead<ComponentProvider>("configureView", ComponentActionListener::class.java, MessageComponent::class.java, ComponentView::class.java)
        { (_, listener: ComponentActionListener, component: MessageComponent, view: ComponentView<MessageComponent>?) ->
            view?.configure(component, this, listener)
        }

        // love
        patcher.patch(WidgetChatListModelMessages.Companion::class.java.declaredMethods.find { it.name == "getMessageItems" }!!)
        {(
            param,
            channel: Channel,
            guildMembers: Map<Long, GuildMember>,
            guildRoles: Map<Long, GuildRole>,
            blockedRelationships: Map<Long, Int>?,
            referencedChannel: Channel?,
            threadStoreState: StoreThreadMessages.ThreadState?,
            message: Message,
            state: StoreMessageState.State?,
            repliedMessages: Map<Long, MessageState>?,
            isBlockedExpanded: Boolean,
            isMinimal: Boolean,
            permissions: Long?,
            allowAnimatedEmojis: Boolean,
            autoPlayGifs: Boolean,
            isRenderEmbedEnabled: Boolean,
            meId: Long,
            isRenderComponentEnabled: Boolean,
            componentStoreState: Map<Long, ComponentStoreState>,
            inviteEmbedModel: InviteEmbedModel?,
            isThreadStarterMessage: Boolean,
            canGuildSeePurchaseFeedbackLoopSystemMessages: Boolean
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
                ComponentTypeExtension.USER_SELECT,
                ComponentTypeExtension.ROLE_SELECT,
                ComponentTypeExtension.MENTIONABLE_SELECT,
                ComponentTypeExtension.CHANNEL_SELECT ->
                    null
                ComponentTypeExtension.SECTION ->
                    null
                ComponentTypeExtension.TEXT_DISPLAY ->
                    TextDisplayComponentView(this.context)
                ComponentTypeExtension.THUMBNAIL ->
                    null
                ComponentTypeExtension.MEDIA_GALLERY ->
                    null
                ComponentTypeExtension.FILE ->
                    null
                ComponentTypeExtension.SEPARATOR ->
                    null
                ComponentTypeExtension.CONTAINER ->
                    null
                else -> null
            }
        }
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}
