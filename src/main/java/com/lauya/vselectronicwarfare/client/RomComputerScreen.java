package com.lauya.vselectronicwarfare.client;

import com.lauya.vselectronicwarfare.block.entity.RomComputerBlockEntity;
import com.lauya.vselectronicwarfare.menu.RomComputerMenu;
import com.lauya.vselectronicwarfare.network.ModNetwork;
import com.lauya.vselectronicwarfare.network.RomComputerConfigPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class RomComputerScreen extends AbstractContainerScreen<RomComputerMenu> {
    private ScriptEditor scriptEditor;

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

        scriptEditor = addRenderableWidget(new ScriptEditor(font, leftPos + 12, topPos + 39, 358, 180,
            Component.translatable("gui.vs_electronic_warfare_fresh.rom_computer.startup_script")));
        scriptEditor.setValue(menu.getInitialScript());

        addRenderableWidget(Button.builder(Component.translatable("gui.vs_electronic_warfare_fresh.rom_computer.save"),
            button -> send(RomComputerConfigPacket.Action.SAVE))
            .bounds(leftPos + 12, topPos + 286, 110, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.vs_electronic_warfare_fresh.rom_computer.start"),
            button -> send(RomComputerConfigPacket.Action.START))
            .bounds(leftPos + 136, topPos + 286, 110, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.vs_electronic_warfare_fresh.rom_computer.shutdown"),
            button -> send(RomComputerConfigPacket.Action.SHUTDOWN))
            .bounds(leftPos + 260, topPos + 286, 110, 20).build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF202326);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + 26, 0xFF30363D);
        graphics.fill(leftPos + 8, topPos + 226, leftPos + imageWidth - 8, topPos + 274, 0xFF16181A);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFE5EDF5, false);
        graphics.drawString(font, Component.translatable("gui.vs_electronic_warfare_fresh.rom_computer.startup_script"),
            12, 24, 0xFFB8C7D9, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        RomComputerBlockEntity computer = menu.getRomComputer(minecraft.player);
        String status = computer != null ? computer.getStatus() : menu.getInitialStatus();
        String error = computer != null ? computer.getError() : menu.getInitialError();
        graphics.drawString(font, Component.translatable("gui.vs_electronic_warfare_fresh.rom_computer.status", status),
            leftPos + 12, topPos + 231, 0xFFB8C7D9, false);
        if (!error.isEmpty()) {
            graphics.drawWordWrap(font, Component.literal(error), leftPos + 12, topPos + 244, 350, 0xFFFF8A80);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void send(RomComputerConfigPacket.Action action) {
        ModNetwork.CHANNEL.sendToServer(new RomComputerConfigPacket(menu.getBlockPos(), action, scriptEditor.getValue()));
    }
}
