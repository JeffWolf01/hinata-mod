package com.example.hinatamod.client;

import com.example.hinatamod.entity.HinataEntity;
import com.example.hinatamod.network.HinataActionPacket;
import com.example.hinatamod.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Menú de interacción: conversar, regalar ramen, seguir, y acciones físicas desbloqueables. */
public class HinataScreen extends Screen {
    private record Entry(int cmd, int required, Button button) {}

    private final HinataEntity hinata;
    private final List<Entry> entries = new ArrayList<>();

    private String line;          // texto completo del subtítulo actual
    private int typed;            // caracteres mostrados (efecto máquina de escribir)
    private int lastSerial;

    public HinataScreen(HinataEntity hinata) {
        super(Component.translatable("entity.hinatamod.hinata"));
        this.hinata = hinata;
        this.lastSerial = hinata.getDialogueSerial();
        this.line = Component.translatable("dialogue.hinatamod.greeting." + hinata.getStage()).getString();
    }

    @Override
    protected void init() {
        entries.clear();
        int top = 46;
        int avail = height - 54 - top - 4;
        int step = Math.max(19, Math.min(24, avail / 7));
        int x = width / 2 + 10;

        int[][] defs = {
                {HinataEntity.CMD_TALK, 0},
                {HinataEntity.CMD_GIFT, 0},
                {HinataEntity.CMD_FOLLOW, 0},
                {HinataEntity.CMD_HEADPAT, HinataEntity.REQ_HEADPAT},
                {HinataEntity.CMD_HOLD, HinataEntity.REQ_HOLD},
                {HinataEntity.CMD_HUG, HinataEntity.REQ_HUG},
                {HinataEntity.CMD_SPECIAL, HinataEntity.REQ_SPECIAL},
        };
        for (int i = 0; i < defs.length; i++) {
            final int cmd = defs[i][0];
            Button b = Button.builder(Component.empty(),
                            btn -> ModNetwork.CHANNEL.sendToServer(new HinataActionPacket(hinata.getId(), cmd)))
                    .bounds(x, top + i * step, 150, step - 3).build();
            addRenderableWidget(b);
            entries.add(new Entry(cmd, defs[i][1], b));
        }
        refreshButtons();
    }

    private Component labelFor(int cmd) {
        return switch (cmd) {
            case HinataEntity.CMD_TALK -> Component.translatable("gui.hinatamod.action.talk");
            case HinataEntity.CMD_GIFT -> Component.translatable("gui.hinatamod.action.gift");
            case HinataEntity.CMD_FOLLOW -> Component.translatable(hinata.isFollowing()
                    ? "gui.hinatamod.action.follow_off" : "gui.hinatamod.action.follow_on");
            case HinataEntity.CMD_HEADPAT -> Component.translatable("gui.hinatamod.action.headpat");
            case HinataEntity.CMD_HOLD -> Component.translatable(hinata.getAction() == HinataEntity.ACTION_HOLD
                    ? "gui.hinatamod.action.hold_off" : "gui.hinatamod.action.hold_on");
            case HinataEntity.CMD_HUG -> Component.translatable("gui.hinatamod.action.hug");
            default -> Component.translatable("gui.hinatamod.action.special");
        };
    }

    private void refreshButtons() {
        int aff = hinata.getAffection();
        for (Entry e : entries) {
            boolean locked = aff < e.required();
            e.button().active = !locked;
            Component label = labelFor(e.cmd());
            e.button().setMessage(locked
                    ? Component.empty().append(label).append(Component.literal(" (" + e.required() + " ♥)"))
                    : label);
            e.button().setTooltip(locked
                    ? Tooltip.create(Component.translatable("gui.hinatamod.locked_tip", e.required()))
                    : null);
        }
    }

    @Override
    public void tick() {
        if (hinata.isRemoved() || minecraft == null || minecraft.player == null
                || hinata.distanceTo(minecraft.player) > 8.0F) {
            onClose();
            return;
        }
        if (hinata.getDialogueSerial() != lastSerial) {
            lastSerial = hinata.getDialogueSerial();
            line = Component.translatable(hinata.getDialogueKey()).getString();
            typed = 0;
        }
        if (typed < line.length()) typed = Math.min(line.length(), typed + 2);
        refreshButtons();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);

        // --- Título y barra de afecto ---
        g.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        int aff = hinata.getAffection();
        int bw = 220, bx = width / 2 - bw / 2, by = 26;
        g.fill(bx - 1, by - 1, bx + bw + 1, by + 9, 0xFF000000);
        g.fill(bx, by, bx + bw, by + 8, 0xFF3A2A3A);
        g.fill(bx, by, bx + bw * aff / HinataEntity.MAX_AFFECTION, by + 8, 0xFFFF6FA5);
        for (int i = 1; i < HinataEntity.STAGE_THRESHOLDS.length; i++) {
            int tx = bx + bw * HinataEntity.STAGE_THRESHOLDS[i] / HinataEntity.MAX_AFFECTION;
            g.fill(tx, by, tx + 1, by + 8, 0xFFFFFFFF);
        }
        g.drawCenteredString(font,
                Component.literal("♥ " + aff + " / " + HinataEntity.MAX_AFFECTION + "  -  ")
                        .append(Component.translatable("gui.hinatamod.stage." + hinata.getStage())),
                width / 2, by + 12, 0xFFC8DC);

        // --- Retrato (usa la textura/expresión actual) ---
        int px = width / 2 - 85;
        int py = height - 62;
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, px, py, 62, px - mouseX, py - 90 - mouseY, hinata);

        super.render(g, mouseX, mouseY, partialTick);

        // --- Caja de diálogo / subtítulos ---
        int boxW = Math.min(width - 20, 400);
        int boxX = width / 2 - boxW / 2;
        int boxY = height - 52;
        g.fill(boxX, boxY, boxX + boxW, boxY + 46, 0xC8000000);
        g.renderOutline(boxX, boxY, boxW, 46, 0xFFFF6FA5);
        g.drawString(font, Component.translatable("entity.hinatamod.hinata"), boxX + 8, boxY + 4, 0xFF9CC4, false);
        List<FormattedCharSequence> lines = font.split(
                Component.literal(line.substring(0, Math.min(typed, line.length()))), boxW - 16);
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            g.drawString(font, lines.get(i), boxX + 8, boxY + 16 + i * 10, 0xFFFFFF, false);
        }
    }
}
