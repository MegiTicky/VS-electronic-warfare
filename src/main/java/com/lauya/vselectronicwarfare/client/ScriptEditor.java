package com.lauya.vselectronicwarfare.client;

import com.lauya.vselectronicwarfare.block.entity.RomComputerBlockEntity;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;

/** A small non-wrapping text editor for CraftOS command scripts. */
public final class ScriptEditor extends AbstractWidget {
    private static final int GUTTER_WIDTH = 32;
    private static final int LINE_HEIGHT = 10;
    private static final int PADDING = 4;

    private final Font font;
    private String value = "";
    private int cursor;
    private int anchor;
    private int firstVisibleLine;
    private int horizontalScroll;

    public ScriptEditor(Font font, int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
        this.font = font;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n');
        cursor = Math.min(cursor, this.value.length());
        anchor = cursor;
        ensureCursorVisible();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible || !isMouseOver(mouseX, mouseY)) return false;
        if (button != InputConstants.MOUSE_BUTTON_LEFT) return false;
        setFocused(true);
        List<LineRange> lines = lines();
        int lineIndex = Math.max(0, Math.min(lines.size() - 1, firstVisibleLine + (int) (mouseY - getY() - PADDING) / LINE_HEIGHT));
        LineRange line = lines.get(lineIndex);
        int targetX = (int) mouseX - getX() - GUTTER_WIDTH - PADDING + horizontalScroll;
        cursor = line.start + characterAtPixel(line.text(), targetX);
        if (!Screen.hasShiftDown()) anchor = cursor;
        ensureCursorVisible();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        if (Screen.hasShiftDown()) {
            horizontalScroll = Math.max(0, horizontalScroll - (int) (delta * 16));
        } else {
            int max = Math.max(0, lines().size() - visibleLineCount());
            firstVisibleLine = Math.max(0, Math.min(max, firstVisibleLine - (int) delta));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused()) return false;
        boolean shift = Screen.hasShiftDown();
        boolean control = Screen.hasControlDown();

        if (control) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_A -> {
                    anchor = 0;
                    cursor = value.length();
                    ensureCursorVisible();
                    return true;
                }
                case GLFW.GLFW_KEY_C -> {
                    copySelection();
                    return true;
                }
                case GLFW.GLFW_KEY_X -> {
                    copySelection();
                    replaceSelection("");
                    return true;
                }
                case GLFW.GLFW_KEY_V -> {
                    replaceSelection(Minecraft.getInstance().keyboardHandler.getClipboard());
                    return true;
                }
                default -> {
                }
            }
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> replaceSelection("\n");
            case GLFW.GLFW_KEY_BACKSPACE -> backspace();
            case GLFW.GLFW_KEY_DELETE -> delete();
            case GLFW.GLFW_KEY_LEFT -> moveCursor(cursor - 1, shift);
            case GLFW.GLFW_KEY_RIGHT -> moveCursor(cursor + 1, shift);
            case GLFW.GLFW_KEY_UP -> moveVertical(-1, shift);
            case GLFW.GLFW_KEY_DOWN -> moveVertical(1, shift);
            case GLFW.GLFW_KEY_HOME -> moveToLineEdge(false, shift);
            case GLFW.GLFW_KEY_END -> moveToLineEdge(true, shift);
            case GLFW.GLFW_KEY_PAGE_UP -> moveVertical(-visibleLineCount(), shift);
            case GLFW.GLFW_KEY_PAGE_DOWN -> moveVertical(visibleLineCount(), shift);
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!isFocused() || Character.isISOControl(codePoint) || Screen.hasControlDown()) return false;
        replaceSelection(String.valueOf(codePoint));
        return true;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        graphics.fill(x, y, x + width, y + height, 0xFF11171D);
        graphics.fill(x, y, x + GUTTER_WIDTH, y + height, 0xFF1B252E);
        int border = isFocused() ? 0xFF6BBECD : 0xFF465562;
        graphics.fill(x, y, x + width, y + 1, border);
        graphics.fill(x, y + height - 1, x + width, y + height, border);
        graphics.fill(x, y, x + 1, y + height, border);
        graphics.fill(x + width - 1, y, x + width, y + height, border);

        List<LineRange> lines = lines();
        int lastLine = Math.min(lines.size(), firstVisibleLine + visibleLineCount());
        graphics.enableScissor(x + GUTTER_WIDTH, y + 1, x + width - 1, y + height - 1);
        for (int index = firstVisibleLine; index < lastLine; index++) {
            LineRange line = lines.get(index);
            int lineY = y + PADDING + (index - firstVisibleLine) * LINE_HEIGHT;
            renderSelection(graphics, line, lineY);
            graphics.drawString(font, line.text(), x + GUTTER_WIDTH + PADDING - horizontalScroll, lineY, 0xFFE5EDF5, false);
        }
        graphics.disableScissor();

        for (int index = firstVisibleLine; index < lastLine; index++) {
            int lineY = y + PADDING + (index - firstVisibleLine) * LINE_HEIGHT;
            String number = Integer.toString(index + 1);
            graphics.drawString(font, number, x + GUTTER_WIDTH - PADDING - font.width(number), lineY, 0xFF7D93A5, false);
        }

        if (isFocused() && Util.getMillis() / 500L % 2L == 0L) renderCursor(graphics, lines);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }

    private void renderSelection(GuiGraphics graphics, LineRange line, int lineY) {
        int selectionStart = Math.min(cursor, anchor);
        int selectionEnd = Math.max(cursor, anchor);
        int start = Math.max(selectionStart, line.start);
        int end = Math.min(selectionEnd, line.end);
        if (start >= end) return;
        int startColumn = start - line.start;
        int endColumn = end - line.start;
        int x = getX() + GUTTER_WIDTH + PADDING + font.width(line.text().substring(0, startColumn)) - horizontalScroll;
        int width = font.width(line.text().substring(startColumn, endColumn));
        graphics.fill(x, lineY - 1, x + width, lineY + font.lineHeight, 0xFF36586B);
    }

    private void renderCursor(GuiGraphics graphics, List<LineRange> lines) {
        int lineIndex = lineIndexFor(cursor, lines);
        if (lineIndex < firstVisibleLine || lineIndex >= firstVisibleLine + visibleLineCount()) return;
        LineRange line = lines.get(lineIndex);
        int column = cursor - line.start;
        int x = getX() + GUTTER_WIDTH + PADDING + font.width(line.text().substring(0, column)) - horizontalScroll;
        int y = getY() + PADDING + (lineIndex - firstVisibleLine) * LINE_HEIGHT;
        graphics.fill(x, y - 1, x + 1, y + font.lineHeight, 0xFFE5EDF5);
    }

    private void backspace() {
        if (cursor != anchor) {
            replaceSelection("");
        } else if (cursor > 0) {
            anchor = cursor - 1;
            replaceSelection("");
        }
    }

    private void delete() {
        if (cursor != anchor) {
            replaceSelection("");
        } else if (cursor < value.length()) {
            anchor = cursor + 1;
            replaceSelection("");
        }
    }

    private void moveCursor(int destination, boolean keepSelection) {
        cursor = Math.max(0, Math.min(value.length(), destination));
        if (!keepSelection) anchor = cursor;
        ensureCursorVisible();
    }

    private void moveVertical(int amount, boolean keepSelection) {
        List<LineRange> lines = lines();
        int current = lineIndexFor(cursor, lines);
        int target = Math.max(0, Math.min(lines.size() - 1, current + amount));
        int columnPixels = font.width(lines.get(current).text().substring(0, cursor - lines.get(current).start));
        cursor = lines.get(target).start + characterAtPixel(lines.get(target).text(), columnPixels);
        if (!keepSelection) anchor = cursor;
        ensureCursorVisible();
    }

    private void moveToLineEdge(boolean end, boolean keepSelection) {
        List<LineRange> lines = lines();
        LineRange line = lines.get(lineIndexFor(cursor, lines));
        cursor = end ? line.end : line.start;
        if (!keepSelection) anchor = cursor;
        ensureCursorVisible();
    }

    private void replaceSelection(String replacement) {
        replacement = replacement.replace("\r\n", "\n").replace('\r', '\n');
        int start = Math.min(cursor, anchor);
        int end = Math.max(cursor, anchor);
        String updated = value.substring(0, start) + replacement + value.substring(end);
        if (updated.getBytes(StandardCharsets.UTF_8).length > RomComputerBlockEntity.MAX_SCRIPT_BYTES) return;
        value = updated;
        cursor = start + replacement.length();
        anchor = cursor;
        ensureCursorVisible();
    }

    private void copySelection() {
        int start = Math.min(cursor, anchor);
        int end = Math.max(cursor, anchor);
        Minecraft.getInstance().keyboardHandler.setClipboard(value.substring(start, end));
    }

    private void ensureCursorVisible() {
        List<LineRange> lines = lines();
        int lineIndex = lineIndexFor(cursor, lines);
        if (lineIndex < firstVisibleLine) firstVisibleLine = lineIndex;
        if (lineIndex >= firstVisibleLine + visibleLineCount()) firstVisibleLine = lineIndex - visibleLineCount() + 1;

        LineRange line = lines.get(lineIndex);
        int cursorX = font.width(line.text().substring(0, cursor - line.start));
        int viewport = width - GUTTER_WIDTH - PADDING * 2;
        if (cursorX < horizontalScroll) horizontalScroll = cursorX;
        if (cursorX > horizontalScroll + viewport - 2) horizontalScroll = cursorX - viewport + 2;
        horizontalScroll = Math.max(0, horizontalScroll);
    }

    private int visibleLineCount() {
        return Math.max(1, (height - PADDING * 2) / LINE_HEIGHT);
    }

    private int characterAtPixel(String line, int pixels) {
        if (pixels <= 0) return 0;
        for (int index = 1; index <= line.length(); index++) {
            if (font.width(line.substring(0, index)) > pixels) return index - 1;
        }
        return line.length();
    }

    private int lineIndexFor(int offset, List<LineRange> lines) {
        for (int index = 0; index < lines.size(); index++) {
            if (offset <= lines.get(index).end) return index;
        }
        return lines.size() - 1;
    }

    private List<LineRange> lines() {
        List<LineRange> lines = new ArrayList<>();
        int start = 0;
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) == '\n') {
                lines.add(new LineRange(start, index, value.substring(start, index)));
                start = index + 1;
            }
        }
        lines.add(new LineRange(start, value.length(), value.substring(start)));
        return lines;
    }

    private record LineRange(int start, int end, String text) {
    }
}
