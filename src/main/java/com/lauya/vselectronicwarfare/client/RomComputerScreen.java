package com.lauya.vselectronicwarfare.client;

import com.lauya.vselectronicwarfare.block.entity.RomComputerBlockEntity;
import com.lauya.vselectronicwarfare.menu.RomComputerMenu;
import com.lauya.vselectronicwarfare.network.ModNetwork;
import com.lauya.vselectronicwarfare.network.RomComputerConfigPacket;
import com.lauya.vselectronicwarfare.network.RomComputerTerminalInputPacket;
import com.lauya.vselectronicwarfare.network.RomComputerTerminalPacket;
import dan200.computercraft.client.gui.widgets.TerminalWidget;
import dan200.computercraft.shared.computer.core.InputHandler;
import dan200.computercraft.shared.computer.terminal.NetworkedTerminal;
import dan200.computercraft.shared.config.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public final class RomComputerScreen extends AbstractContainerScreen<RomComputerMenu> {
    private ScriptEditor scriptEditor;
    private final List<AbstractWidget> configurationWidgets = new ArrayList<>();
    private NetworkedTerminal terminal;
    private TerminalWidget terminalWidget;
    private boolean terminalTab;
    private boolean terminalInteractive;

    public RomComputerScreen(RomComputerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 382;
        imageHeight = 320;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 12;
        titleLabelY = 10;

        scriptEditor = addConfigurationWidget(new ScriptEditor(font, leftPos + 12, topPos + 58, 358, 161,
            Component.translatable("gui.vs_electronic_warfare.rom_computer.startup_script")));
        scriptEditor.setValue(menu.getInitialScript());

        addConfigurationWidget(Button.builder(Component.translatable("gui.vs_electronic_warfare.rom_computer.save"),
            button -> send(RomComputerConfigPacket.Action.SAVE))
            .bounds(leftPos + 12, topPos + 286, 110, 20).build());
        addConfigurationWidget(Button.builder(Component.translatable("gui.vs_electronic_warfare.rom_computer.start"),
            button -> send(RomComputerConfigPacket.Action.START))
            .bounds(leftPos + 136, topPos + 286, 110, 20).build());
        addConfigurationWidget(Button.builder(Component.translatable("gui.vs_electronic_warfare.rom_computer.shutdown"),
            button -> send(RomComputerConfigPacket.Action.SHUTDOWN))
            .bounds(leftPos + 260, topPos + 286, 110, 20).build());

        terminal = new NetworkedTerminal(Config.computerTermWidth, Config.computerTermHeight, true);
        terminalWidget = addRenderableWidget(new TerminalWidget(terminal, new TerminalInput(), leftPos + 12, topPos + 58));
        addRenderableWidget(Button.builder(Component.translatable("gui.vs_electronic_warfare.rom_computer.configuration"),
            button -> setTerminalTab(false)).bounds(leftPos + 12, topPos + 34, 112, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.vs_electronic_warfare.rom_computer.terminal"),
            button -> setTerminalTab(true)).bounds(leftPos + 128, topPos + 34, 112, 20).build());
        setTerminalTab(false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF202326);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + 26, 0xFF30363D);
        if (!terminalTab) graphics.fill(leftPos + 8, topPos + 226, leftPos + imageWidth - 8, topPos + 274, 0xFF16181A);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFE5EDF5, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        if (!terminalTab) {
            RomComputerBlockEntity computer = menu.getRomComputer(minecraft.player);
            String status = computer != null ? computer.getStatus() : menu.getInitialStatus();
            String error = computer != null ? computer.getError() : menu.getInitialError();
            graphics.drawString(font, Component.translatable("gui.vs_electronic_warfare.rom_computer.status", status),
                leftPos + 12, topPos + 231, 0xFFB8C7D9, false);
            if (!error.isEmpty()) graphics.drawWordWrap(font, Component.literal(error), leftPos + 12, topPos + 244, 350, 0xFFFF8A80);
        } else if (!terminalInteractive) {
            graphics.drawString(font, Component.translatable("gui.vs_electronic_warfare.rom_computer.terminal_read_only"),
                leftPos + 12, topPos + imageHeight - 24, 0xFFB8C7D9, false);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Keep the configured inventory key available to the focused script editor.
        if (scriptEditor.isFocused() && minecraft.options.keyInventory.matches(keyCode, scanCode)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        terminalWidget.update();
    }

    public void updateTerminal(RomComputerTerminalPacket packet) {
        if (!packet.blockPos().equals(menu.getBlockPos())) return;
        packet.terminal().apply(terminal);
        terminalInteractive = packet.interactive();
        terminalWidget.active = terminalInteractive;
        terminalWidget.setFocused(terminalTab && terminalInteractive);
    }

    private <T extends AbstractWidget> T addConfigurationWidget(T widget) {
        configurationWidgets.add(widget);
        return addRenderableWidget(widget);
    }

    private void setTerminalTab(boolean terminalTab) {
        this.terminalTab = terminalTab;
        for (AbstractWidget widget : configurationWidgets) {
            widget.visible = !terminalTab;
            widget.active = !terminalTab;
        }
        terminalWidget.visible = terminalTab;
        terminalWidget.active = terminalTab && terminalInteractive;
        terminalWidget.setFocused(terminalTab && terminalInteractive);
    }

    private void send(RomComputerConfigPacket.Action action) {
        ModNetwork.CHANNEL.sendToServer(new RomComputerConfigPacket(menu.getBlockPos(), action, scriptEditor.getValue()));
    }

    private final class TerminalInput implements InputHandler {
        private void send(RomComputerTerminalInputPacket.Action action, int key, boolean repeat, int x, int y, String text) {
            if (terminalInteractive) ModNetwork.CHANNEL.sendToServer(new RomComputerTerminalInputPacket(menu.getBlockPos(), action, key, repeat, x, y, text));
        }

        @Override
        public void queueEvent(String event, Object[] arguments) {
            String text = arguments.length == 0 ? "" : String.valueOf(arguments[0]);
            send(event.equals("paste") ? RomComputerTerminalInputPacket.Action.PASTE : RomComputerTerminalInputPacket.Action.CHAR,
                0, false, 0, 0, text);
        }

        @Override public void keyDown(int key, boolean repeat) { send(RomComputerTerminalInputPacket.Action.KEY_DOWN, key, repeat, 0, 0, ""); }
        @Override public void keyUp(int key) { send(RomComputerTerminalInputPacket.Action.KEY_UP, key, false, 0, 0, ""); }
        @Override public void mouseClick(int button, int x, int y) { send(RomComputerTerminalInputPacket.Action.MOUSE_CLICK, button, false, x, y, ""); }
        @Override public void mouseUp(int button, int x, int y) { send(RomComputerTerminalInputPacket.Action.MOUSE_UP, button, false, x, y, ""); }
        @Override public void mouseDrag(int button, int x, int y) { send(RomComputerTerminalInputPacket.Action.MOUSE_DRAG, button, false, x, y, ""); }
        @Override public void mouseScroll(int direction, int x, int y) { send(RomComputerTerminalInputPacket.Action.MOUSE_SCROLL, direction, false, x, y, ""); }
        @Override public void shutdown() { }
        @Override public void turnOn() { }
        @Override public void reboot() { }
    }
}
