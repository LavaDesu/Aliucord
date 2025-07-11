@file:Suppress("MISSING_DEPENDENCY_CLASS", "MISSING_DEPENDENCY_SUPERCLASS")

package com.aliucord.coreplugins.componentsv2.views

import android.content.Context
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
import com.aliucord.Logger
import com.aliucord.coreplugins.componentsv2.BotUiComponentV2Entry
import com.aliucord.coreplugins.componentsv2.ComponentV2Type
import com.aliucord.coreplugins.componentsv2.models.ThumbnailMessageComponent
import com.aliucord.utils.DimenUtils.dp
import com.discord.utilities.color.ColorCompat
import com.discord.utilities.embed.EmbedResourceUtils
import com.discord.utilities.images.MGImages
import com.discord.widgets.botuikit.ComponentProvider
import com.discord.widgets.botuikit.views.ComponentActionListener
import com.discord.widgets.botuikit.views.ComponentView
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemBotComponentRow
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.card.MaterialCardView
import com.lytefast.flexinput.R

class ThumbnailComponentView(ctx: Context)
    : ConstraintLayout(ctx), ComponentView<ThumbnailMessageComponent> {
    private val embedThumbnailMaxSize = (ctx.resources.getDimension(R.d.embed_thumbnail_max_size) * 1.5).toInt()

    private val imageView = SimpleDraweeView(ctx, null, 0, R.i.UiKit_ImageView).apply { }
    private val spoilerView = SpoilerView(ctx, 2).apply {
        layoutParams = LayoutParams(0, 0).apply {
            bottomToBottom = PARENT_ID
            endToEnd = PARENT_ID
            startToStart = PARENT_ID
            topToTop = PARENT_ID
        }
    }

    init {
        val view = MaterialCardView(ctx).apply {
            radius = 8.dp.toFloat()
            elevation = 0f
            setCardBackgroundColor(ColorCompat.getThemedColor(ctx, R.b.colorBackgroundPrimary))
            layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            addView(ConstraintLayout(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                addView(imageView)
                addView(spoilerView)
            })
        }

        addView(view)
    }

    // Reference: WidgetChatListAdapterItemEmbed.configureEmbedThumbnail
    override fun configure(component: ThumbnailMessageComponent, provider: ComponentProvider, listener: ComponentActionListener) {
        val item = listener as WidgetChatListAdapterItemBotComponentRow
        val entry = item.entry
        if (entry !is BotUiComponentV2Entry) {
            Logger("ComponentsV2").warn("configured thumbnail with non-v2 entry")
            return
        }

        val (width, height) = EmbedResourceUtils.INSTANCE.calculateScaledSize(
            component.media.width,
            component.media.height,
            embedThumbnailMaxSize,
            embedThumbnailMaxSize,
            resources,
            0
        )
        imageView.apply {
            if (layoutParams.width != width || layoutParams.height != height)
                layoutParams = layoutParams.apply {
                    this.width = width
                    this.height = height
                }
            MGImages.`setImage$default`(
                this,
                EmbedResourceUtils.INSTANCE.getPreviewUrls(component.media.proxyUrl, width, height, true), // z2: shouldAnimate
                0,
                0,
                false,
                null,
                null,
                null,
                252,
                null
            )
        }

        spoilerView.configure(entry, component)
    }

    override fun type() = ComponentV2Type.THUMBNAIL
}
