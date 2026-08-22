package com.emutune.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards the layout contract that [OpticSurface] stacks its children vertically.
 *
 * A previous bug made this component render children in a Box, which painted every
 * child at the top-left on top of one another — the visible symptom was overlapping
 * text on the Home and game-detail screens. This test asserts children are laid out
 * top-to-bottom with no vertical overlap.
 */
@RunWith(AndroidJUnit4::class)
class OpticSurfaceStackingTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun childrenStackVerticallyWithoutOverlap() {
        rule.setContent {
            OpticSurface {
                Text("Header")
                Row {
                    Text("First metric")
                    Text("Second metric")
                }
                Text("Footer")
            }
        }

        val header = rule.onNodeWithText("Header").getBoundsInRoot()
        val footer = rule.onNodeWithText("Footer").getBoundsInRoot()

        // Header must sit entirely above Footer; if they overlapped (the Box bug),
        // header.bottom would be > footer.top.
        assertTrue(
            "OpticSurface children overlapped: header.bottom=${header.bottom}, footer.top=${footer.top}",
            header.bottom <= footer.top,
        )
    }
}
