package com.aliucord.coreplugins.decorations.displayname

import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.View.OnLayoutChangeListener
import android.widget.TextView
import androidx.core.graphics.TypefaceCompat
import com.aliucord.*
import com.aliucord.api.PatcherAPI
import com.aliucord.coreplugins.decorations.DecorationsSettings
import com.aliucord.patcher.*
import com.aliucord.utils.RxUtils.subscribe
import com.aliucord.utils.ViewUtils.findViewById
import com.aliucord.utils.accessField
import com.aliucord.wrappers.users.displayNameStyles
import com.discord.api.user.DisplayNameStyle
import com.discord.databinding.WidgetChannelMembersListItemUserBinding
import com.discord.models.user.User
import com.discord.stores.StoreStream
import com.discord.utilities.spans.TypefaceSpanCompat
import com.discord.widgets.channels.memberlist.adapter.ChannelMembersListAdapter
import com.discord.widgets.channels.memberlist.adapter.ChannelMembersListViewHolderMember
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage
import com.discord.widgets.chat.list.entries.ChatListEntry
import com.discord.widgets.chat.list.entries.MessageEntry
import com.discord.widgets.user.UserNameFormatterKt
import com.discord.widgets.user.profile.UserProfileHeaderView
import com.discord.widgets.user.profile.UserProfileHeaderViewModel
import rx.subjects.BehaviorSubject
import java.io.File
import java.util.WeakHashMap

private val logger = Logger("Decorations/DisplayNameStyles")

private val ChannelMembersListViewHolderMember.binding by accessField<WidgetChannelMembersListItemUserBinding>()

private const val BASE_GFONTS = "https://github.com/google/fonts/raw/e324c91423626034f1e10098081182bb88e340db/ofl"
enum class FontStyle(val url: String?, val isVariable: Boolean = false) {
    Default(null),
    // -- Unused
    Bangers("$BASE_GFONTS/bangers/Bangers-Regular.ttf"),
    // Tempo
    BioRhyme("$BASE_GFONTS/biorhyme/BioRhyme%5Bwdth,wght%5D.ttf", isVariable = true),
    // Sakura
    CherryBomb("$BASE_GFONTS/cherrybombone/CherryBombOne-Regular.ttf"),
    // Jellybean
    Chicle("$BASE_GFONTS/chicle/Chicle-Regular.ttf"),
    // -- Unused
    Compagnon("https://gitlab.com/velvetyne/compagnon/-/raw/4f2344df5adb6eaf9ffd9215c5406c0729fb7aa1/fonts/Compagnon-Medium.otf?inline=false"),
    // Modern
    MuseoModerno("$BASE_GFONTS/museomoderno/MuseoModerno%5Bwght%5D.ttf", isVariable = true),
    // Medieval
    NeoCastel("https://files.catbox.moe/npwf2e.otf"),
    // 8Bit
    Pixelify("$BASE_GFONTS/pixelifysans/PixelifySans%5Bwght%5D.ttf", isVariable = true),
    // -- Unused
    Ribes("https://github.com/collletttivo/ribes/raw/e5f58f6ef719ff69b599a3155c66f4cecaed0a0f/fonts/Ribes-Black.otf"),
    // Vampyre
    Sinistre("https://github.com/collletttivo/sinistre/raw/3308f8b884a066951a4da38586abae3b247bf915/fonts/Sinistre-Bold.otf"),
    // -- Unused (near identical to BioRhyme)
    ZillaSlab("$BASE_GFONTS/zillaslab/ZillaSlab-Bold.ttf"),
    ;

    companion object {
        fun from(value: Int) = when (value) {
            1 -> Bangers
            2 -> BioRhyme
            3 -> CherryBomb
            4 -> Chicle
            5 -> Compagnon
            6 -> MuseoModerno
            7 -> NeoCastel
            8 -> Pixelify
            9 -> Ribes
            10 -> Sinistre
            11 -> Default
            12 -> ZillaSlab
            else -> {
                logger.warn("Unknown font style $value")
                Default
            }
        }
    }
}

enum class EffectStyle(val value: Int) {
    Solid(1),
    Gradient(2),
    Neon(3),
    Toon(4),
    Pop(5),
    Glow(6),
    ;

    companion object {
        fun from(value: Int) = when (value) {
            1 -> Solid
            2 -> Gradient
            3 -> Neon
            4 -> Toon
            5 -> Pop
            6 -> Glow
            else -> {
                logger.warn("Unknown effect style $value")
                Solid
            }
        }
    }
}

internal object FontHandler {
    // val cachedTypefaces = mutableMapOf<FontStyle, Typeface>()
    private val cachedTypefaces = mutableMapOf<FontStyle, BehaviorSubject<Typeface>>()

    fun fetch(style: FontStyle, onValue: (Typeface) -> Unit) {
        val subject = cachedTypefaces.getOrPut(style) {
            logger.info("finding $style")
            val subject = BehaviorSubject.k0<Typeface>()
            Utils.threadPool.execute {
                val path = File(Utils.appActivity.cacheDir, "fonts/${style.name}.ttf")
                if (!path.exists()) {
                    path.parentFile?.mkdirs()
                    logger.info("fetching $style")
                    Http.Request.newDiscordRequest(style.url).execute().saveToFile(path)
                    logger.info("fetched $style to $path")
                }
                logger.info("found $style")
                val typeface = Typeface.createFromFile(path)
                // If we get androidx core 1.9.0 we can use TypefaceCompat.create with a weight
                if (style.isVariable && Build.VERSION.SDK_INT >= 28) {
                    subject.onNext(Typeface.create(typeface, 500, false))
                } else {
                    subject.onNext(TypefaceCompat.create(Utils.appContext, typeface, Typeface.BOLD))
                }
            }
            subject
        }

        subject.z() // .first()
            .subscribe(onValue)
    }
}

internal object DisplayNameStyles {
    private val defaultTypeface = WeakHashMap<TextView, Typeface>()
    private val listeners = WeakHashMap<TextView, OnLayoutChangeListener>()

    fun patch(patcher: PatcherAPI) {
        if (!DecorationsSettings.enableDisplayNames) return

        patchMemberList(patcher)
        patchProfileHeader(patcher)
        patchMessageAuthor(patcher)
    }

    private fun configureOn(view: TextView, styleData: DisplayNameStyle?, applyEffect: Boolean) {
        defaultTypeface[view]?.let { view.typeface = it }
        listeners.remove(view)?.let { view.removeOnLayoutChangeListener(it) }
        view.paint.shader = null

        if (styleData == null)
            return;

        val font = FontStyle.from(styleData.fontId)
        val effect = EffectStyle.from(styleData.effectId)

        font.url?.let {
            FontHandler.fetch(font) {
                defaultTypeface.putIfAbsent(view, view.typeface)
                view.typeface = it
            }
        }

        if (applyEffect) {
            when (effect) {
                EffectStyle.Solid -> {}
                EffectStyle.Toon,
                EffectStyle.Pop,
                EffectStyle.Neon -> {
                    view.setTextColor((styleData.colors[0] + 0xFF000000).toInt())
                }
                EffectStyle.Glow,
                EffectStyle.Gradient -> {
                    val (from, to) = styleData.colors.map { (it + 0xFF000000).toInt() }
                    val list = OnLayoutChangeListener { v, left, top, right, bottom, _, _, _, _ ->
                        if (v !is TextView)
                            return@OnLayoutChangeListener
                        v.paint.shader = LinearGradient(
                            /* x0 */ 0f,
                            /* y0 */ 0f,
                            /* x1 */ right.toFloat() - left.toFloat(),
                            /* y1 */ bottom.toFloat() - top.toFloat(),
                            /* colorFrom */ from,
                            /* colorTo */ to,
                            Shader.TileMode.REPEAT
                        )
                    }
                    view.addOnLayoutChangeListener(list)
                    listeners[view] = list
                }
            }
        }
    }

    private fun patchMemberList(patcher: PatcherAPI) {
        // Patches the method that configures the username
        patcher.after<ChannelMembersListViewHolderMember>(
            "bind",
            ChannelMembersListAdapter.Item.Member::class.java,
            Function0::class.java,
        ) { (_, member: ChannelMembersListAdapter.Item.Member) ->
            val usernameView = binding.f
            val usernameTextView = usernameView.j.c
            val data = StoreStream.getUsers().users[member.userId]?.displayNameStyles
            configureOn(usernameTextView, data, false)
        }
    }

    private fun patchProfileHeader(patcher: PatcherAPI) {
        // Patches the method that configures the username in profile header
        patcher.after<UserProfileHeaderView>(
            "updateViewState",
            UserProfileHeaderViewModel.ViewState.Loaded::class.java,
        ) { (_, state: UserProfileHeaderViewModel.ViewState.Loaded) ->
            val binding = UserProfileHeaderView.`access$getBinding$p`(this)
            val usernameView = binding.j
            val usernameTextView = usernameView.j.c
            state.user.displayNameStyles?.let {
                logger.info("${state.user.username}: $it")
            }
            configureOn(usernameTextView, state.user.displayNameStyles, true)
        }

        // Remove the custom typeface if the user has display name styles, so it displays properly
        patcher.patch(UserNameFormatterKt::class.java.declaredMethods.first { it.name == "getSpannableForUserNameWithDiscrim" })
        { (param, user: User) ->
            val styles = user.displayNameStyles
                ?: return@patch

            val res = param.result as SpannableStringBuilder
            val colorSpan = res.getSpans(0, res.length, ForegroundColorSpan::class.java)
            val typefaceSpan = res.getSpans(0, res.length, TypefaceSpanCompat::class.java)
            if (styles.colors.isNotEmpty()) {
                colorSpan.getOrNull(0)?.let { res.removeSpan(it) }
            }
            if (styles.fontId != 11) {
                typefaceSpan.getOrNull(0)?.let { res.removeSpan(it) }
            }
        }
    }

    private fun patchMessageAuthor(patcher: PatcherAPI) {
        // Configures the guild tag for the message author
        patcher.after<WidgetChatListAdapterItemMessage>(
            "onConfigure",
            Int::class.javaPrimitiveType!!,
            ChatListEntry::class.java,
        ) { (_, _: Int, entry: MessageEntry) ->
            val username = itemView.findViewById<TextView?>("chat_list_adapter_item_text_name")
                ?: return@after
            configureOn(username, entry.message.author.displayNameStyles, false)
        }

        // Configures the guild tag for the reply preview
        patcher.before<WidgetChatListAdapterItemMessage>(
            "configureReplyPreview",
            MessageEntry::class.java,
        ) { (_, entry: MessageEntry) ->
            val referencedAuthor = entry.message.referencedMessage?.e()

            val replyUsername = itemView.findViewById<TextView?>("chat_list_adapter_item_text_decorator_reply_name")
                ?: return@before
            configureOn(replyUsername, referencedAuthor?.displayNameStyles, false)
        }
    }
}
