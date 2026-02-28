package com.optimistswe.mementolauncher.service

import android.view.accessibility.AccessibilityNodeInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ShortsBlockerServiceTest {

    private lateinit var service: ShortsBlockerService

    @Before
    fun setup() {
        service = ShortsBlockerService()
    }

    private fun createMockNode(
        desc: String? = null,
        className: String? = null,
        isSelected: Boolean = false,
        isClickable: Boolean = false,
        children: List<AccessibilityNodeInfo> = emptyList(),
        parentNode: AccessibilityNodeInfo? = null
    ): AccessibilityNodeInfo {
        val node = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { node.contentDescription } returns desc
        every { node.className } returns className
        every { node.isSelected } returns isSelected
        every { node.isClickable } returns isClickable
        every { node.childCount } returns children.size
        every { node.parent } returns parentNode
        for (i in children.indices) {
            every { node.getChild(i) } returns children[i]
        }
        return node
    }

    @Test
    fun `isYouTubeShortsVisible returns true when Shorts tab is selected`() {
        val shortsTab = createMockNode(desc = "Shorts", isSelected = true)
        val rootNode = createMockNode(children = listOf(shortsTab))

        val result = service.isYouTubeShortsVisible(rootNode)

        assertTrue(result)
        verify { shortsTab.recycle() }
    }

    @Test
    fun `isYouTubeShortsVisible returns false when Shorts tab is not selected`() {
        val shortsTab = createMockNode(desc = "Shorts", isSelected = false)
        val rootNode = createMockNode(children = listOf(shortsTab))

        val result = service.isYouTubeShortsVisible(rootNode)

        assertFalse(result)
        verify { shortsTab.recycle() }
    }

    @Test
    fun `isInstagramReelsVisible returns true when Reels tab is selected`() {
        val reelsTab = createMockNode(desc = "Reels", isSelected = true)
        val rootNode = createMockNode(children = listOf(reelsTab))

        val result = service.isInstagramReelsVisible(rootNode)

        assertTrue(result)
        verify { reelsTab.recycle() }
    }

    @Test
    fun `attemptClickHomeTab returns true and clicks when Home button found`() {
        val homeButton = createMockNode(desc = "Home", isClickable = true)
        val rootNode = createMockNode(children = listOf(homeButton))

        val result = service.attemptClickHomeTab(rootNode)

        assertTrue(result)
        verify { homeButton.performAction(AccessibilityNodeInfo.ACTION_CLICK) }
        verify { homeButton.recycle() }
    }

    @Test
    fun `attemptClickHomeTab searches parent tree if Home text found but not clickable`() {
        val clickableParent = createMockNode(isClickable = true)
        val nonClickableHomeText = createMockNode(desc = "Home", isClickable = false, parentNode = clickableParent)
        val rootNode = createMockNode(children = listOf(nonClickableHomeText))

        val result = service.attemptClickHomeTab(rootNode)

        assertTrue(result)
        verify { clickableParent.performAction(AccessibilityNodeInfo.ACTION_CLICK) }
    }

    // ═══════════════════════════════════════════
    // Additional edge cases
    // ═══════════════════════════════════════════

    @Test
    fun `isYouTubeShortsVisible returns false for empty node tree`() {
        val rootNode = createMockNode(children = emptyList())
        assertFalse(service.isYouTubeShortsVisible(rootNode))
    }

    @Test
    fun `isInstagramReelsVisible returns false for empty node tree`() {
        val rootNode = createMockNode(children = emptyList())
        assertFalse(service.isInstagramReelsVisible(rootNode))
    }

    @Test
    fun `isInstagramReelsVisible returns false when Reels tab is not selected`() {
        val reelsTab = createMockNode(desc = "Reels", isSelected = false)
        val rootNode = createMockNode(children = listOf(reelsTab))

        assertFalse(service.isInstagramReelsVisible(rootNode))
    }

    @Test
    fun `isYouTubeShortsVisible finds Shorts in nested children`() {
        val shortsTab = createMockNode(desc = "Shorts", isSelected = true)
        val middleNode = createMockNode(children = listOf(shortsTab))
        val rootNode = createMockNode(children = listOf(middleNode))

        assertTrue(service.isYouTubeShortsVisible(rootNode))
    }

    @Test
    fun `isInstagramReelsVisible finds Reels in nested children`() {
        val reelsTab = createMockNode(desc = "Reels", isSelected = true)
        val middleNode = createMockNode(children = listOf(reelsTab))
        val rootNode = createMockNode(children = listOf(middleNode))

        assertTrue(service.isInstagramReelsVisible(rootNode))
    }

    @Test
    fun `attemptClickHomeTab returns false when no Home tab exists`() {
        val otherNode = createMockNode(desc = "Settings")
        val rootNode = createMockNode(children = listOf(otherNode))

        assertFalse(service.attemptClickHomeTab(rootNode))
    }

    @Test
    fun `attemptClickHomeTab returns false for empty tree`() {
        val rootNode = createMockNode(children = emptyList())
        assertFalse(service.attemptClickHomeTab(rootNode))
    }

    @Test
    fun `attemptClickHomeTab finds Home with inicio description`() {
        val inicioButton = createMockNode(desc = "inicio", isClickable = true)
        val rootNode = createMockNode(children = listOf(inicioButton))

        assertTrue(service.attemptClickHomeTab(rootNode))
        verify { inicioButton.performAction(AccessibilityNodeInfo.ACTION_CLICK) }
    }

    @Test
    fun `isYouTubeShortsVisible handles null child gracefully`() {
        val rootNode = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { rootNode.childCount } returns 1
        every { rootNode.getChild(0) } returns null

        assertFalse(service.isYouTubeShortsVisible(rootNode))
    }

    @Test
    fun `isYouTubeShortsVisible ignores non-Shorts selected tab`() {
        val homeTab = createMockNode(desc = "Home", isSelected = true)
        val rootNode = createMockNode(children = listOf(homeTab))

        assertFalse(service.isYouTubeShortsVisible(rootNode))
    }

    @Test
    fun `isYouTubeShortsVisible case-insensitive for Shorts`() {
        val shortsTab = createMockNode(desc = "SHORTS", isSelected = true)
        val rootNode = createMockNode(children = listOf(shortsTab))

        // desc is lowercased in the service, "SHORTS".lowercase() = "shorts"
        assertTrue(service.isYouTubeShortsVisible(rootNode))
    }
}
