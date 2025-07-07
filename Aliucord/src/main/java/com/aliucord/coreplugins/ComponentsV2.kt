package com.aliucord.coreplugins

import android.content.Context
import android.view.ViewGroup
import com.aliucord.coreplugins.componentsv2.ComponentTypeExtension
import com.aliucord.entities.CorePlugin
import com.aliucord.patcher.*
import com.aliucord.updater.ManagerBuild
import com.discord.api.botuikit.*
import com.discord.models.botuikit.ActionRowMessageComponent
import com.discord.models.botuikit.MessageComponent
import com.discord.widgets.botuikit.*
import com.discord.widgets.botuikit.views.ActionRowComponentView
import com.discord.widgets.botuikit.views.ButtonComponentView
import com.discord.widgets.botuikit.views.select.SelectComponentView

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
                    ActionRowMessageComponent(layout.type, index, components)
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
                    null
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
