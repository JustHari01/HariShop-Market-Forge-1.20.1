package hari.harishop.quilt;

import hari.harishop.HariShopMod;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;

public class HariShopQuiltMod implements ModInitializer {

    @Override
    public void onInitialize(ModContainer mod) {
        HariShopMod.init();
    }

}
