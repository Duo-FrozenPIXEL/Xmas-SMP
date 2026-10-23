package de.xmassmp.fabric;

import de.xmassmp.effect.DisplayHandle;
import de.xmassmp.effect.DisplaySpec;
import de.xmassmp.effect.DisplayTransform;
import de.xmassmp.util.Vec3;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.DisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Brightness;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Display-Entity über Polymers Virtual Entities: Es existiert nur als Pakete bei den Clients,
 * wird also nie mit der Welt gespeichert und kann nach einem Absturz nicht liegen bleiben.
 */
final class FabricDisplay implements DisplayHandle {

    private final ElementHolder holder = new ElementHolder();
    private final DisplayElement element;
    private final net.minecraft.world.phys.Vec3 anchor;
    private final boolean centerBlock;
    private boolean removed;

    FabricDisplay(ServerLevel level, Vec3 position, DisplaySpec spec) {
        this.anchor = new net.minecraft.world.phys.Vec3(position.x(), position.y(), position.z());
        this.centerBlock = spec.kind() == DisplaySpec.Kind.BLOCK && spec.centered();
        this.element = spec.kind() == DisplaySpec.Kind.BLOCK
                ? new BlockDisplayElement(BuiltInRegistries.BLOCK.getValue(McIds.parse(spec.id())).defaultBlockState())
                : new ItemDisplayElement(new ItemStack(BuiltInRegistries.ITEM.getValue(McIds.parse(spec.id()))));
        if (spec.glowColor() != null) {
            element.setGlowing(true);
            element.setGlowColorOverride(spec.glowColor().value());
        }
        if (spec.brightness() >= 0) {
            element.setBrightness(new Brightness(spec.brightness(), spec.brightness()));
        }
        applyTransform(spec.transform());
        holder.addElement(element);
        ChunkAttachment.ofTicking(holder, level, anchor);
    }

    @Override
    public void moveTo(Vec3 position, int ticks) {
        if (removed) {
            return;
        }
        element.setTeleportDuration(ticks);
        element.setOffset(new net.minecraft.world.phys.Vec3(position.x(), position.y(), position.z()).subtract(anchor));
    }

    @Override
    public void transform(DisplayTransform transform, int ticks) {
        if (removed) {
            return;
        }
        element.setInterpolationDuration(ticks);
        applyTransform(transform);
        element.startInterpolation();
    }

    @Override
    public void remove() {
        if (!removed) {
            removed = true;
            holder.destroy();
        }
    }

    @Override
    public boolean isRemoved() {
        return removed;
    }

    private void applyTransform(DisplayTransform t) {
        Quaternionf rotation = new Quaternionf().rotateYXZ(
                (float) Math.toRadians(t.yaw()), (float) Math.toRadians(t.pitch()), (float) Math.toRadians(t.roll()));
        Vector3f scale = new Vector3f((float) t.scale().x(), (float) t.scale().y(), (float) t.scale().z());
        Vector3f translation = new Vector3f((float) t.translation().x(), (float) t.translation().y(),
                (float) t.translation().z());
        if (centerBlock) {
            // Blockmodelle beginnen an der Ecke: halbe Größe zurückschieben, mitgedreht.
            translation.add(rotation.transform(new Vector3f(scale).mul(-0.5f)));
        }
        element.setTranslation(translation);
        element.setScale(scale);
        element.setLeftRotation(rotation);
    }
}
