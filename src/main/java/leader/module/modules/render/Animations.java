package leader.module.modules.render;

import leader.module.Module;
import leader.property.properties.BooleanProperty;
import leader.property.properties.FloatProperty;
import leader.property.properties.ModeProperty;

public class Animations extends Module {

    public final ModeProperty mode = new ModeProperty(
            "mode",
            2,
            new String[]{
                    "1.8",
                    "Swing",
                    "Push",
            }
    );
    public final BooleanProperty cancelEquip = new BooleanProperty("Cancel Equip", false);
    public final BooleanProperty cancelEquipBlockingOnly = new BooleanProperty("Cancel Equip Blocking Only", true, () -> this.cancelEquip.getValue());
    public final FloatProperty itemSize = new FloatProperty("Item Size", 0.0F, -0.5F, 0.5F);
    public final FloatProperty itemFov = new FloatProperty("Item Fov", 0.0F, -5.0F, 5.0F);
    public final FloatProperty itemPosX = new FloatProperty("Item Pos X", 0.0F, -1.0F, 1.0F);
    public final FloatProperty itemPosY = new FloatProperty("Item Pos Y", 0.0F, -1.0F, 1.0F);
    public final FloatProperty itemPosZ = new FloatProperty("Item Pos Z", 0.0F, -1.0F, 1.0F);
    public final FloatProperty blockPosX = new FloatProperty("Block Pos X", 0.0F, -1.0F, 1.0F);
    public final FloatProperty blockPosY = new FloatProperty("Block Pos Y", 0.0F, -1.0F, 1.0F);
    public final FloatProperty blockPosZ = new FloatProperty("Block Pos Z", 0.0F, -1.0F, 1.0F);
    public final FloatProperty swingSpeed = new FloatProperty("Swing Speed", 1.0F, 0.1F, 5.0F);

    public Animations() {
        super("Animations", false);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }
}
