@file:Suppress("MISSING_DEPENDENCY_CLASS", "MISSING_DEPENDENCY_SUPERCLASS")

package com.aliucord.coreplugins.componentsv2.views

import android.content.Context
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
import com.aliucord.coreplugins.componentsv2.ComponentV2Type
import com.aliucord.coreplugins.componentsv2.models.SectionMessageComponent
import com.aliucord.utils.DimenUtils.dp
import com.aliucord.widgets.LinearLayout
import com.discord.widgets.botuikit.ComponentProvider
import com.discord.widgets.botuikit.views.ComponentActionListener
import com.discord.widgets.botuikit.views.ComponentView
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemBotComponentRowKt

class SectionComponentView(val ctx: Context) : ConstraintLayout(ctx), ComponentView<SectionMessageComponent> {
    companion object {
        private val accessoryViewId = View.generateViewId()
    }

    private val mainView = LinearLayout(ctx).apply {
        layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            constrainedWidth = true
            horizontalBias = 0f
            topToTop = PARENT_ID
            startToStart = PARENT_ID
            endToStart = accessoryViewId
            marginEnd = 16.dp
        }
        this@SectionComponentView.addView(this)
    }
    private var accessoryView = FrameLayout(ctx).apply {
        id = accessoryViewId
        layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            topToTop = PARENT_ID
            endToEnd = PARENT_ID
        }
        this@SectionComponentView.addView(this)
    }

    override fun configure(component: SectionMessageComponent, provider: ComponentProvider, listener: ComponentActionListener) {
        val configuredViews = component.components.mapIndexed { index, child ->
            provider.getConfiguredComponentView(listener, child, mainView, index)
        }.filterNotNull()
        WidgetChatListAdapterItemBotComponentRowKt.replaceViews(mainView, configuredViews)

        val accessoryComponent = provider.getConfiguredComponentView(listener, component.accessory, accessoryView, 0)
        accessoryComponent?.let {
            WidgetChatListAdapterItemBotComponentRowKt.replaceViews(accessoryView, listOf(accessoryComponent))
        }
    }

    override fun type() = ComponentV2Type.SECTION
}
