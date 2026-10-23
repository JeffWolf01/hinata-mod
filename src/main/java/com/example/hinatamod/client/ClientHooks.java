package com.example.hinatamod.client;

import com.example.hinatamod.entity.HinataEntity;
import net.minecraft.client.Minecraft;

/** Solo se carga en el cliente (se invoca vía DistExecutor). */
public class ClientHooks {
    public static void openHinataScreen(HinataEntity hinata) {
        Minecraft.getInstance().setScreen(new HinataScreen(hinata));
    }
}
