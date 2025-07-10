package com.aliucord.coreplugins.componentsv2.views

import android.content.Context
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
import com.aliucord.coreplugins.componentsv2.BotUiComponentV2Entry
import com.aliucord.coreplugins.componentsv2.models.SpoilableMessageComponent
import com.aliucord.utils.DimenUtils.dp
import com.discord.stores.StoreStream
import com.discord.utilities.color.ColorCompat
import com.discord.widgets.botuikit.views.ComponentView
import com.lytefast.flexinput.R

/**
 * A message component view that can be spoilered.
 *
 * @param ctx Context
 * @param type 1 for full (spoiler text and button), 2 for mini (eye icon)
 */
abstract class SpoilableComponentView<T : SpoilableMessageComponent>(ctx: Context, type: Int)
    : ConstraintLayout(ctx), ComponentView<T> {
    protected val spoilerView = ConstraintLayout(ctx).apply {
        visibility = GONE
        setBackgroundColor(ColorCompat.getThemedColor(ctx, R.b.theme_chat_spoiler_bg))
        layoutParams = LayoutParams(0, 0).apply {
            bottomToBottom = PARENT_ID
            endToEnd = PARENT_ID
            startToStart = PARENT_ID
            topToTop = PARENT_ID
        }
        isClickable = true

        val innerView = when (type) {
            1 -> {
                CardView(ctx).apply {
                    elevation = ctx.resources.getDimension(R.d.app_elevation)
                    setCardBackgroundColor(ColorCompat.getThemedColor(ctx, R.b.colorBackgroundFloating))
                    radius = 16.dp.toFloat()

                    layoutParams = ConstraintLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        startToStart = PARENT_ID
                        endToEnd = PARENT_ID
                        topToTop = PARENT_ID
                        bottomToBottom = PARENT_ID
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
            }
            2 -> {
                ImageView(ctx).apply {
                    setImageResource(R.e.ic_spoiler)
                    layoutParams = ConstraintLayout.LayoutParams(0, 0).apply {
                        startToStart = PARENT_ID
                        endToEnd = PARENT_ID
                        topToTop = PARENT_ID
                        bottomToBottom = PARENT_ID
                        dimensionRatio = "1:1"
                        matchConstraintPercentWidth = 0.5f
                    }
                }
            }
            else -> throw IllegalArgumentException("Invalid spoiler view type")
        }
        addView(innerView)
    }

    protected fun configureSpoiler(entry: BotUiComponentV2Entry, component: T) {
        val spoiled = entry.state?.visibleSpoilerEmbedMap?.containsKey(component.id) ?: false

        spoilerView.setOnClickListener {
            spoilerView.setOnClickListener(null)
            spoilerView.animate()
                .withEndAction {
                    StoreStream.getMessageState().revealSpoilerEmbed(entry.message.id, component.id)
                }
                .alpha(0f)
        }
        spoilerView.visibility = if (component.spoiler && !spoiled) VISIBLE else GONE
        spoilerView.alpha = 1f
    }
}
