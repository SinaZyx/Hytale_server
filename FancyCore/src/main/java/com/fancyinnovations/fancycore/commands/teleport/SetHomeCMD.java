package com.fancyinnovations.fancycore.commands.teleport;

import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.api.player.Home;
import com.fancyinnovations.fancycore.api.teleport.Location;
import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.fancyinnovations.fancycore.utils.NumberUtils;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.RelativeDoublePosition;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class SetHomeCMD extends AbstractWorldCommand {

    protected final RequiredArg<String> nameArg = this.withRequiredArg("home", "name of the new home", ArgTypes.STRING);
    private final OptionalArg<RelativeDoublePosition> positionArg = this.withOptionalArg("position", "position to set",
            ArgTypes.RELATIVE_POSITION);
    private final DefaultArg<Vector3f> rotationArg = this.withDefaultArg("rotation", "rotation to set",
            ArgTypes.ROTATION, Vector3f.FORWARD, "forward looking direction");

    public SetHomeCMD() {
        super("sethome", "Sets your home point to your current location");
        addAliases("createhome");
        requirePermission("fancycore.commands.sethome");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull World world, @Nonnull Store<EntityStore> store) {
        if (!ctx.isPlayer()) {
            FancyCorePlugin.get().getTranslationService()
                .getMessage("error.command.player_only")
                .sendTo(ctx.sender());
            return;
        }

        FancyPlayer fp = FancyPlayerService.get().getByUUID(ctx.sender().getUuid());
        if (fp == null) {
            FancyCorePlugin.get().getTranslationService()
                .getMessage("error.player.not_found")
                .sendTo(ctx.sender());
            return;
        }
        Ref<EntityStore> playerRef = ctx.senderAsPlayerRef();

        Vector3d position;
        if (this.positionArg.provided(ctx)) {
            RelativeDoublePosition relativePosition = this.positionArg.get(ctx);
            position = relativePosition.getRelativePosition(ctx, world, store);
        } else {
            TransformComponent transformComponent = store.getComponent(playerRef,
                    TransformComponent.getComponentType());
            position = transformComponent.getPosition().clone();
        }

        Vector3f rotation;
        if (this.rotationArg.provided(ctx)) {
            rotation = this.rotationArg.get(ctx);
        } else {
            HeadRotation headRotationComponent = store.getComponent(playerRef, HeadRotation.getComponentType());
            rotation = headRotationComponent.getRotation();
        }

        String name = nameArg.get(ctx);
        if (fp.getData().getHome(name) != null) {
            FancyCorePlugin.get().getTranslationService()
                .getMessage("teleport.home.already_exists", fp.getLanguage())
                .replace("name", name)
                .sendTo(fp);
            return;
        }

        Location homeLocation = new Location(
                world.getName(),
                position.getX(),
                position.getY(),
                position.getZ(),
                rotation.getYaw(),
                rotation.getPitch());
        fp.getData().addHome(new Home(name, homeLocation));

        FancyCorePlugin.get().getTranslationService()
            .getMessage("teleport.home.set", fp.getLanguage())
            .replace("name", name)
            .replace("x", NumberUtils.formatNumber(homeLocation.x()))
            .replace("y", NumberUtils.formatNumber(homeLocation.y()))
            .replace("z", NumberUtils.formatNumber(homeLocation.z()))
            .replace("world", homeLocation.worldName())
            .sendTo(fp);
    }
}
