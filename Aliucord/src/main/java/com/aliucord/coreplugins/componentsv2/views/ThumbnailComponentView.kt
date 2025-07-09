@file:Suppress("MISSING_DEPENDENCY_CLASS", "MISSING_DEPENDENCY_SUPERCLASS")

package com.aliucord.coreplugins.componentsv2.views

import android.content.Context
import android.graphics.drawable.ColorDrawable
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
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
import com.lytefast.flexinput.R
import b.f.g.f.c as RoundingParams

class ThumbnailComponentView(val ctx: Context) : ConstraintLayout(ctx), ComponentView<ThumbnailMessageComponent> {
    private val embedThumbnailMaxSize = ctx.resources.getDimension(R.d.embed_thumbnail_max_size).toInt()

    val view = SimpleDraweeView(ctx, null, 0, R.i.UiKit_ImageView).apply {
        hierarchy = hierarchy.apply {
            a(ColorDrawable(ColorCompat.getThemedColor(ctx, R.b.colorBackgroundPrimary))) // setPlaceholderImage
            o(0, ContextCompat.getDrawable(ctx, R.e.drawable_overlay_image_square)) // setOverlayImage
            s(RoundingParams.a(8.dp.toFloat())) // setRoundingParam(RoundingParams.fromCornerRadius(float))
        }

        this@ThumbnailComponentView.addView(this)
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
        view.apply {
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
    }

    override fun type() = ComponentV2Type.THUMBNAIL
}
