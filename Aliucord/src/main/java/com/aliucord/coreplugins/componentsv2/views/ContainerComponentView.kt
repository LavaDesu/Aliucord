@file:Suppress("MISSING_DEPENDENCY_CLASS", "MISSING_DEPENDENCY_SUPERCLASS")

package com.aliucord.coreplugins.componentsv2.views

import android.content.Context
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
import androidx.core.graphics.ColorUtils
import com.aliucord.Logger
import com.aliucord.coreplugins.componentsv2.BotUiComponentV2Entry
import com.aliucord.coreplugins.componentsv2.ComponentV2Type
import com.aliucord.coreplugins.componentsv2.models.ContainerMessageComponent
import com.aliucord.utils.DimenUtils.dp
import com.aliucord.widgets.LinearLayout
import com.discord.utilities.color.ColorCompat
import com.discord.widgets.botuikit.ComponentProvider
import com.discord.widgets.botuikit.views.ComponentActionListener
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemBotComponentRow
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemBotComponentRowKt
import com.google.android.material.card.MaterialCardView
import com.lytefast.flexinput.R

class ContainerComponentView(val ctx: Context)
    : SpoilableComponentView<ContainerMessageComponent>(ctx, 1) {
    private val cardView = MaterialCardView(ctx).apply {
        radius = 8.dp.toFloat()
        elevation = 0f
        setCardBackgroundColor(ColorCompat.getThemedColor(ctx, R.b.colorBackgroundSecondary))
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            topToTop = PARENT_ID
            bottomToBottom = PARENT_ID
            startToStart = PARENT_ID
            endToEnd = PARENT_ID
            marginEnd = ctx.resources.getDimension(R.d.chat_cell_horizontal_spacing_padding).toInt()
        }
        this@ContainerComponentView.addView(this)
    }

    private val innerCardView = ConstraintLayout(ctx).apply {
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        cardView.addView(this)
    }

    private val dividerId = View.generateViewId()
    private val accentDivider = View(ctx).apply {
        id = dividerId
        layoutParams = LayoutParams(3.dp, 0).apply {
            bottomToBottom = PARENT_ID
            startToStart = PARENT_ID
            topToTop = PARENT_ID
        }
        innerCardView.addView(this)
    }

    private val contentView = LinearLayout(ctx).apply {
        setPadding(8.dp, 8.dp, 8.dp, 8.dp)
        layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            startToEnd = dividerId
            endToEnd = PARENT_ID
            topToTop = PARENT_ID
            constrainedWidth = true
        }
        innerCardView.addView(this)
    }

    init {
        innerCardView.addView(spoilerView)
    }

    override fun configure(component: ContainerMessageComponent, provider: ComponentProvider, listener: ComponentActionListener) {
        val item = listener as WidgetChatListAdapterItemBotComponentRow
        val entry = item.entry
        if (entry !is BotUiComponentV2Entry) {
            Logger("ComponentsV2").warn("configured container with non-v2 entry")
            return
        }

        val configuredViews = component.components.mapIndexed { index, child ->
            provider.getConfiguredComponentView(listener, child, contentView, index)
        }
        WidgetChatListAdapterItemBotComponentRowKt.replaceViews(contentView, configuredViews)

        val color = component.accentColor?.let { ColorUtils.setAlphaComponent(it, 255) }
            ?: ColorCompat.getThemedColor(ctx, R.b.colorBackgroundModifierAccent)
        accentDivider.setBackgroundColor(color)

        configureSpoiler(entry, component)
    }

    override fun type() = ComponentV2Type.CONTAINER
}
