@file:Suppress("MISSING_DEPENDENCY_CLASS", "MISSING_DEPENDENCY_SUPERCLASS")

package com.aliucord.coreplugins.componentsv2.views

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
import androidx.core.graphics.ColorUtils
import com.aliucord.Logger
import com.aliucord.coreplugins.componentsv2.BotUiComponentV2Entry
import com.aliucord.coreplugins.componentsv2.ComponentV2Type
import com.aliucord.coreplugins.componentsv2.models.ContainerMessageComponent
import com.aliucord.utils.DimenUtils.dp
import com.aliucord.widgets.LinearLayout
import com.discord.stores.StoreStream
import com.discord.utilities.color.ColorCompat
import com.discord.widgets.botuikit.ComponentProvider
import com.discord.widgets.botuikit.views.ComponentActionListener
import com.discord.widgets.botuikit.views.ComponentView
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemBotComponentRow
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemBotComponentRowKt
import com.google.android.material.card.MaterialCardView
import com.lytefast.flexinput.R

class ContainerComponentView(val ctx: Context) : ConstraintLayout(ctx), ComponentView<ContainerMessageComponent> {
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

    private val spoilerView = FrameLayout(ctx).apply {
        visibility = GONE
        setBackgroundColor(ColorCompat.getThemedColor(ctx, R.b.theme_chat_spoiler_bg))
        layoutParams = LayoutParams(0, 0).apply {
            bottomToBottom = PARENT_ID
            endToEnd = PARENT_ID
            startToStart = PARENT_ID
            topToTop = PARENT_ID
        }
        isClickable = true

        val textContainer = CardView(ctx).apply {
            elevation = ctx.resources.getDimension(R.d.app_elevation)
            setCardBackgroundColor(ColorCompat.getThemedColor(ctx, R.b.colorBackgroundFloating))
            radius = 16.dp.toFloat()

            layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }

            val textView = TextView(ctx, null, 0, R.i.UiKit_TextView_H2).apply {
                setText(R.h.spoiler)
                isAllCaps = true
                setPadding(8.dp, 4.dp, 8.dp, 4.dp)
                setTextColor(ColorCompat.getThemedColor(ctx, R.b.colorTextNormal))
                layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    marginStart = 4.dp
                    marginEnd = 4.dp
                }
            }
            addView(textView)
        }
        addView(textContainer)

        innerCardView.addView(this)
    }

    override fun configure(component: ContainerMessageComponent, provider: ComponentProvider, listener: ComponentActionListener) {
        val item = listener as WidgetChatListAdapterItemBotComponentRow
        val entry = item.entry
        if (entry !is BotUiComponentV2Entry) {
            Logger("ComponentsV2").warn("configured thumbnail with non-v2 entry")
            return
        }

        val configuredViews = component.components.mapIndexed { index, child ->
            item.adapter.botUiComponentProvider.getConfiguredComponentView(listener, child, contentView, index)
        }
        WidgetChatListAdapterItemBotComponentRowKt.replaceViews(contentView, configuredViews)

        val color = component.accentColor?.let { ColorUtils.setAlphaComponent(it, 255) }
            ?: ColorCompat.getThemedColor(ctx, R.b.colorBackgroundModifierAccent)
        accentDivider.setBackgroundColor(color)

        if (spoilerView.animation == null) {
            spoilerView.setOnClickListener {
                spoilerView.setOnClickListener(null)
                spoilerView.animate()
                    .withEndAction {
                        StoreStream.getMessageState().revealSpoilerEmbed(entry.message.id, component.id)
                    }
                    .alpha(0f)
            }
            val spoiled = entry.state?.visibleSpoilerEmbedMap?.containsKey(component.id) ?: false
            spoilerView.visibility = if (component.spoiler && !spoiled) VISIBLE else GONE
        }
    }

    override fun type() = ComponentV2Type.CONTAINER
}
