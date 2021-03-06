package io.github.a5h73y.listeners;

import io.github.a5h73y.Carz;
import io.github.a5h73y.enums.Permissions;
import io.github.a5h73y.enums.PurchaseType;
import io.github.a5h73y.model.Car;
import io.github.a5h73y.other.Utils;
import io.github.a5h73y.utility.PermissionUtils;
import io.github.a5h73y.utility.StringUtils;
import io.github.a5h73y.utility.TranslationUtils;
import io.github.a5h73y.utility.ValidationUtils;
import org.bukkit.ChatColor;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class SignListener implements Listener {

    private final Carz carz;

    public SignListener(Carz carz) {
        this.carz = carz;
    }

    /**
     * When a Sign is created.
     * Check if it was a Car's related Sign.
     * If it's a valid PurchaseType, display a price.
     * @param event
     */
    @EventHandler
    public void onSignCreate(SignChangeEvent event) {
        if (!event.getLine(0).equalsIgnoreCase("[carz]")) {
            return;
        }

        Player player = event.getPlayer();

        if (!PermissionUtils.hasPermission(player, Permissions.CREATE_SIGN)) {
            event.getBlock().breakNaturally();
            event.setCancelled(true);
            return;
        }

        // if it's a valid command, break, otherwise it's unknown, so cancel
        switch (event.getLine(1).toLowerCase()) {
            case "refuel":
            case "purchase":
            case "upgrade":
                break;
            default:
                TranslationUtils.sendTranslation("Error.UnknownSignCommand", player);
                player.sendMessage(Carz.getPrefix() + "Valid signs: refuel, purchase, upgrade");
                event.getBlock().breakNaturally();
                event.setCancelled(true);
                return;
        }

        String title = StringUtils.standardizeText(event.getLine(1));
        player.sendMessage(Carz.getPrefix() + title + " sign created");

        event.setLine(0, Carz.getInstance().getSettings().getSignHeader());
        event.setLine(3, ChatColor.RED + String.valueOf(PurchaseType.fromString(title).getCost()));
    }

    /**
     * When a Carz sign is broken.
     * Attempt to protect the sign if invalid.
     * @param event
     */
    @EventHandler
    public void onSignBreak(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        if (!(event.getClickedBlock().getBlockData() instanceof Sign)) {
            return;
        }

        if (!Carz.getInstance().getConfig().getBoolean("Carz.SignProtection")) {
            return;
        }

        String[] lines = ((Sign) event.getClickedBlock().getState()).getLines();

        if (!ChatColor.stripColor(lines[0]).equalsIgnoreCase("[carz]")) {
            return;
        }

        if (!PermissionUtils.hasStrictPermission(event.getPlayer(), Permissions.ADMIN)) {
            TranslationUtils.sendTranslation("SignProtected", event.getPlayer());
            event.setCancelled(true);
        } else {
            event.getClickedBlock().breakNaturally();
            event.getPlayer().sendMessage("Sign Removed!"); //TODO translation
        }
    }

    /**
     * On Carz sign interaction.
     * Attempt to process a purchase if requested.
     * @param event
     */
    @EventHandler
    public void onSignInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (!(event.getClickedBlock().getBlockData() instanceof Sign)) {
            return;
        }

        Sign sign = (Sign) event.getClickedBlock().getState();
        String[] lines = sign.getLines();

        String signHeader = ChatColor.stripColor(
                StringUtils.colour(Carz.getInstance().getSettings().getSignHeader()));

        if (!ChatColor.stripColor(lines[0]).equalsIgnoreCase(signHeader)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        switch (lines[1].toLowerCase()) {
            case "refuel":
                if (!ValidationUtils.canPurchaseFuel(player)) {
                    return;
                }

                Vehicle currentVehicle = (Vehicle) player.getVehicle();
                Car car = carz.getCarController().getCar(currentVehicle.getEntityId());
                carz.getFuelController().refuel(car, player);
                break;

            case "purchase":
                if (!ValidationUtils.canPurchaseCar(player)) {
                    return;
                }

                Utils.givePlayerOwnedCar(player);
                TranslationUtils.sendTranslation("Car.Purchased", player);
                break;

            case "upgrade":
                if (!ValidationUtils.canPurchaseUpgrade(player)) {
                    return;
                }

                carz.getCarController().upgradeCarSpeed(player);
                break;

            default:
                TranslationUtils.sendTranslation("Error.UnknownSignCommand", player);
        }
    }
}
