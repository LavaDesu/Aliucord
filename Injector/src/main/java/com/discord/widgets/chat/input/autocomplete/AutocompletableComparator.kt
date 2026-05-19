package com.discord.widgets.chat.input.autocomplete

import com.discord.api.channel.ChannelUtils
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Replaces Discord's stock autocomplete comparator to fix bugs such as some entries not appearing. For instance
 * user suggestion entries with the same nickname/display name will only appear once.
 */
@Suppress("unused")
class AutocompletableComparator : Comparator<Autocompletable> {
    override fun compare(a: Autocompletable, b: Autocompletable): Int {
        if (a::class != b::class) {
            return AutocompletableKt.getSortIndex(a).compareTo(AutocompletableKt.getSortIndex(b))
        }

        return when {
            check<ApplicationCommandChoiceAutocompletable>(a, b) -> {
                compareValuesBy(a, b) { it.choice.a().lowercase()}
            }

            // *New*: Compare by name first, then compare by app id, then compare by app name
            // Originally it only compares by name if id matches, then by application name if it doesn't
            check<ApplicationCommandAutocompletable>(a, b) -> {
                compareValuesBy(
                    a, b,
                    { it.command.name },
                    { it.application?.id },
                )
            }

            check<ApplicationPlaceholder>(a, b) -> {
                compareValuesBy(a, b) { it.application.name.lowercase() }
            }

            check<ChannelAutocompletable>(a, b) -> {
                compareValuesBy(
                    a, b,
                    { ChannelUtils.c(it.channel).lowercase() }, // ChannelUtils.getDisplayName
                    { it.channel.k() }, // Channel.id
                )
            }

            check<EmojiAutocompletable>(a, b) -> {
                compareValuesBy(a, b) { it.emoji.firstName } // TODO: Check if .lower() should be used
            }

            check<GlobalRoleAutocompletable>(a, b) -> {
                compareValuesBy(a, b) { it.text.lowercase() }
            }

            // *New*: replace default name comparison
            check<RoleAutocompletable>(a, b) -> {
                compareValuesBy(
                    a, b,
                    { it.role.g().lowercase() }, // Role.getName()
                    { it.role.id } // *New*: additionally compare by id
                )
            }

            // *New*: replace default username#discrim comparison
            check<UserAutocompletable>(a, b) -> {
                compareValuesBy(
                    a, b,
                    { (it.nickname ?: it.user.username).lowercase() }, // Compare by nickname/display name first
                    { it.user.username.lowercase() }, // Then compare by username
                    { it.user.discriminator }, // Then compare by discrim
                )
            }

            check<ApplicationCommandLoadingPlaceholder>(a, b) -> 0
            check<EmojiUpsellPlaceholder>(a, b) -> 0

            else -> throw NoWhenBranchMatchedException()
        }
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun <reified T : Autocompletable> check(a: Autocompletable, b: Autocompletable): Boolean {
        contract {
            returns(true) implies (a is T)
            returns(true) implies (b is T)
        }
        return a is T;
    }
}
